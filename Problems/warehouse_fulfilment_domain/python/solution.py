"""One-hour warehouse fulfilment core (Python 3.9, standard library only).

The RLock represents one local database transaction. Production replacements are
conditional writes/version columns plus durable command receipts and outbox rows.
"""

from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass, field
from enum import Enum
from threading import RLock
from typing import Dict, List, Optional, Protocol, Sequence, Tuple


class WarehouseError(Exception): pass
class AllocationError(WarehouseError): pass
class InvalidState(WarehouseError): pass
class ClaimConflict(WarehouseError): pass
class IdempotencyConflict(WarehouseError): pass
class QuantityError(WarehouseError): pass


def _required(value: str, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("{} must not be blank".format(name))
    return value.strip()


def _positive(value: int, name: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
        raise ValueError("{} must be a positive integer".format(name))
    return value


@dataclass(frozen=True)
class OrderLineDemand:
    line_id: str
    sku: str
    quantity: int

    def __post_init__(self) -> None:
        object.__setattr__(self, "line_id", _required(self.line_id, "line_id"))
        object.__setattr__(self, "sku", _required(self.sku, "sku"))
        _positive(self.quantity, "quantity")


@dataclass(frozen=True)
class WarehouseAvailability:
    warehouse_id: str
    bin_id: str
    sku: str
    quantity: int

    def __post_init__(self) -> None:
        object.__setattr__(self, "warehouse_id", _required(self.warehouse_id, "warehouse_id"))
        object.__setattr__(self, "bin_id", _required(self.bin_id, "bin_id"))
        object.__setattr__(self, "sku", _required(self.sku, "sku"))
        _positive(self.quantity, "quantity")


@dataclass(frozen=True)
class AllocationDraft:
    line_id: str
    sku: str
    warehouse_id: str
    bin_id: str
    quantity: int


class AllocationPolicy(Protocol):
    def plan(
        self,
        demands: Sequence[OrderLineDemand],
        availability: Sequence[WarehouseAvailability],
    ) -> Tuple[AllocationDraft, ...]:
        ...


class SplitFirstFitAllocator:
    """Pure deterministic policy; a line may split across warehouse/bin sources."""

    def plan(
        self,
        demands: Sequence[OrderLineDemand],
        availability: Sequence[WarehouseAvailability],
    ) -> Tuple[AllocationDraft, ...]:
        capacity: Dict[Tuple[str, str, str], int] = {}
        for source in availability:
            key = (source.warehouse_id, source.bin_id, source.sku)
            capacity[key] = capacity.get(key, 0) + source.quantity
        result: List[AllocationDraft] = []
        for demand in sorted(demands, key=lambda item: (item.line_id, item.sku)):
            remaining = demand.quantity
            for warehouse_id, bin_id, sku in sorted(capacity):
                if not remaining:
                    break
                if sku != demand.sku:
                    continue
                key = (warehouse_id, bin_id, sku)
                take = min(remaining, capacity[key])
                if take:
                    result.append(AllocationDraft(demand.line_id, sku, warehouse_id, bin_id, take))
                    capacity[key] -= take
                    remaining -= take
            if remaining:
                raise AllocationError("line {} is short by {}".format(demand.line_id, remaining))
        return tuple(result)


class TaskStatus(str, Enum):
    AVAILABLE = "available"
    CLAIMED = "claimed"
    COMPLETED = "completed"
    SHORT_PICKED = "short_picked"


class PackageStatus(str, Enum):
    OPEN = "open"
    SEALED = "sealed"
    SHIPPED = "shipped"


class FulfilmentStatus(str, Enum):
    ALLOCATED = "allocated"
    PICKING = "picking"
    PARTIALLY_SHIPPED = "partially_shipped"
    SHIPPED = "shipped"
    EXCEPTION = "exception"


@dataclass
class FulfilmentLine:
    line_id: str
    sku: str
    requested: int
    allocated: int = 0
    picked: int = 0
    packed: int = 0
    shipped: int = 0

    def change(self, allocated: int = 0, picked: int = 0,
               packed: int = 0, shipped: int = 0) -> None:
        candidate = (
            self.allocated + allocated,
            self.picked + picked,
            self.packed + packed,
            self.shipped + shipped,
        )
        new_allocated, new_picked, new_packed, new_shipped = candidate
        if not 0 <= new_shipped <= new_packed <= new_picked <= new_allocated <= self.requested:
            raise QuantityError("line quantity conservation would be violated")
        self.allocated, self.picked, self.packed, self.shipped = candidate


@dataclass
class PickTask:
    task_id: str
    order_id: str
    line_id: str
    sku: str
    warehouse_id: str
    bin_id: str
    quantity: int
    picked: int = 0
    short: int = 0
    status: TaskStatus = TaskStatus.AVAILABLE
    claimed_by: Optional[str] = None
    claim_token: Optional[str] = None
    claim_epoch: int = 0

    def claim(self, worker_id: str) -> None:
        if self.status is TaskStatus.CLAIMED and self.claimed_by == worker_id:
            return
        if self.status is not TaskStatus.AVAILABLE:
            raise ClaimConflict("task is not available")
        self.claim_epoch += 1
        self.claimed_by = worker_id
        self.claim_token = "{}:{}".format(self.task_id, self.claim_epoch)
        self.status = TaskStatus.CLAIMED

    def require_claim(self, worker_id: str, token: str) -> None:
        if self.status is not TaskStatus.CLAIMED or self.claimed_by != worker_id or self.claim_token != token:
            raise ClaimConflict("worker does not own the current claim token")

    def scan(self, worker_id: str, token: str, quantity: int) -> None:
        self.require_claim(worker_id, token)
        if quantity > self.quantity - self.picked:
            raise QuantityError("scan exceeds unfinished task quantity")
        self.picked += quantity
        if self.picked == self.quantity:
            self.status = TaskStatus.COMPLETED

    def mark_short(self, worker_id: str, token: str) -> int:
        self.require_claim(worker_id, token)
        missing = self.quantity - self.picked
        if not missing:
            raise InvalidState("completed task cannot be short-picked")
        self.short = missing
        self.status = TaskStatus.SHORT_PICKED
        return missing


@dataclass
class Package:
    package_id: str
    order_id: str
    warehouse_id: str
    contents: Dict[str, int] = field(default_factory=dict)
    status: PackageStatus = PackageStatus.OPEN
    tracking_reference: Optional[str] = None

    def add(self, line_id: str, quantity: int) -> None:
        if self.status is not PackageStatus.OPEN:
            raise InvalidState("only an open package accepts contents")
        self.contents[line_id] = self.contents.get(line_id, 0) + quantity

    def seal(self) -> None:
        if self.status is not PackageStatus.OPEN or not self.contents:
            raise InvalidState("only a non-empty open package can be sealed")
        self.status = PackageStatus.SEALED

    def ship(self, tracking_reference: str) -> None:
        if self.status is not PackageStatus.SEALED:
            raise InvalidState("carrier confirmation requires a sealed package")
        self.tracking_reference = tracking_reference
        self.status = PackageStatus.SHIPPED


@dataclass
class FulfilmentOrder:
    order_id: str
    lines: Dict[str, FulfilmentLine]
    tasks: Dict[str, PickTask] = field(default_factory=dict)
    packages: Dict[str, Package] = field(default_factory=dict)
    exceptions: Dict[str, str] = field(default_factory=dict)

    @property
    def status(self) -> FulfilmentStatus:
        lines = tuple(self.lines.values())
        if self.exceptions:
            return FulfilmentStatus.EXCEPTION
        if all(line.shipped == line.requested for line in lines):
            return FulfilmentStatus.SHIPPED
        if any(line.shipped for line in lines):
            return FulfilmentStatus.PARTIALLY_SHIPPED
        if any(line.picked for line in lines):
            return FulfilmentStatus.PICKING
        return FulfilmentStatus.ALLOCATED


class WarehouseFulfilmentService:
    """Coordinates policy and aggregates inside an exercise-scale transaction."""

    def __init__(self, allocator: Optional[AllocationPolicy] = None) -> None:
        self._allocator = allocator or SplitFirstFitAllocator()
        self._orders: Dict[str, FulfilmentOrder] = {}
        self._task_to_order: Dict[str, str] = {}
        self._package_to_order: Dict[str, str] = {}
        self._receipts: Dict[Tuple[str, str], Tuple[object, ...]] = {}
        self._lock = RLock()

    def create_order(self, order_id: str, demands: Sequence[OrderLineDemand],
                     availability: Sequence[WarehouseAvailability]) -> FulfilmentOrder:
        order_id = _required(order_id, "order_id")
        if not demands or len({item.line_id for item in demands}) != len(demands):
            raise ValueError("order needs unique, non-empty lines")
        drafts = self._allocator.plan(demands, availability)  # fail before mutation
        with self._lock:
            if order_id in self._orders:
                raise InvalidState("order already exists")
            order = FulfilmentOrder(order_id, {
                item.line_id: FulfilmentLine(item.line_id, item.sku, item.quantity)
                for item in demands
            })
            self._orders[order_id] = order
            for draft in drafts:
                self._append_task(order, draft)
                order.lines[draft.line_id].change(allocated=draft.quantity)
            return deepcopy(order)

    def claim_task(self, task_id: str, worker_id: str) -> PickTask:
        worker_id = _required(worker_id, "worker_id")
        with self._lock:
            _, task = self._task(task_id)
            task.claim(worker_id)  # exactly one winner under the transaction lock
            return deepcopy(task)

    def scan(self, scan_id: str, task_id: str, worker_id: str,
             claim_token: str, quantity: int) -> PickTask:
        quantity = _positive(quantity, "quantity")
        fingerprint = (task_id, worker_id, claim_token, quantity)
        with self._lock:
            order, task = self._task(task_id)
            if self._replay("scan", scan_id, fingerprint):
                return deepcopy(task)
            task.scan(worker_id, claim_token, quantity)
            order.lines[task.line_id].change(picked=quantity)
            self._record("scan", scan_id, fingerprint)
            return deepcopy(task)

    def short_pick(self, command_id: str, task_id: str, worker_id: str,
                   claim_token: str, reason: str) -> FulfilmentOrder:
        reason = _required(reason, "reason")
        fingerprint = (task_id, worker_id, claim_token, reason)
        with self._lock:
            order, task = self._task(task_id)
            if self._replay("short", command_id, fingerprint):
                return deepcopy(order)
            missing = task.mark_short(worker_id, claim_token)
            order.lines[task.line_id].change(allocated=-missing)
            order.exceptions[task.line_id] = reason
            self._record("short", command_id, fingerprint)
            return deepcopy(order)

    def recover_shortage(self, command_id: str, order_id: str, line_id: str,
                         availability: Sequence[WarehouseAvailability]) -> FulfilmentOrder:
        sources = tuple(sorted((x.warehouse_id, x.bin_id, x.sku, x.quantity) for x in availability))
        fingerprint = (order_id, line_id, sources)
        with self._lock:
            order = self._order(order_id)
            if self._replay("recover", command_id, fingerprint):
                return deepcopy(order)
            line = self._line(order, line_id)
            missing = line.requested - line.allocated
            if line_id not in order.exceptions or not missing:
                raise InvalidState("line has no recoverable shortage")
            drafts = self._allocator.plan([OrderLineDemand(line_id, line.sku, missing)], availability)
            for draft in drafts:
                self._append_task(order, draft)
                line.change(allocated=draft.quantity)
            order.exceptions.pop(line_id)
            self._record("recover", command_id, fingerprint)
            return deepcopy(order)

    def create_package(self, package_id: str, order_id: str,
                       warehouse_id: str) -> Package:
        package_id = _required(package_id, "package_id")
        warehouse_id = _required(warehouse_id, "warehouse_id")
        with self._lock:
            if package_id in self._package_to_order:
                raise InvalidState("package already exists")
            order = self._order(order_id)
            package = Package(package_id, order_id, warehouse_id)
            order.packages[package_id] = package
            self._package_to_order[package_id] = order_id
            return deepcopy(package)

    def pack(self, command_id: str, package_id: str,
             line_id: str, quantity: int) -> Package:
        quantity = _positive(quantity, "quantity")
        fingerprint = (package_id, line_id, quantity)
        with self._lock:
            order, package = self._package(package_id)
            if self._replay("pack", command_id, fingerprint):
                return deepcopy(package)
            line = self._line(order, line_id)
            picked_here = sum(t.picked for t in order.tasks.values()
                              if t.line_id == line_id and t.warehouse_id == package.warehouse_id)
            packed_here = sum(p.contents.get(line_id, 0) for p in order.packages.values()
                              if p.warehouse_id == package.warehouse_id)
            if quantity > picked_here - packed_here:
                raise QuantityError("package would consume units not picked here")
            package.add(line_id, quantity)
            line.change(packed=quantity)
            self._record("pack", command_id, fingerprint)
            return deepcopy(package)

    def seal_package(self, command_id: str, package_id: str) -> Package:
        fingerprint = (package_id,)
        with self._lock:
            _, package = self._package(package_id)
            if self._replay("seal", command_id, fingerprint):
                return deepcopy(package)
            package.seal()
            self._record("seal", command_id, fingerprint)
            return deepcopy(package)

    def ship_package(self, command_id: str, package_id: str,
                     tracking_reference: str) -> Package:
        tracking_reference = _required(tracking_reference, "tracking_reference")
        fingerprint = (package_id, tracking_reference)
        with self._lock:
            order, package = self._package(package_id)
            if self._replay("ship", command_id, fingerprint):
                return deepcopy(package)
            package.ship(tracking_reference)  # this command records confirmed hand-off
            for line_id, quantity in package.contents.items():
                order.lines[line_id].change(shipped=quantity)
            self._record("ship", command_id, fingerprint)
            return deepcopy(package)

    def get_order(self, order_id: str) -> FulfilmentOrder:
        with self._lock:
            return deepcopy(self._order(order_id))

    def _append_task(self, order: FulfilmentOrder, draft: AllocationDraft) -> None:
        task_id = "{}:pick:{}".format(order.order_id, len(order.tasks) + 1)
        task = PickTask(task_id, order.order_id, draft.line_id, draft.sku,
                        draft.warehouse_id, draft.bin_id, draft.quantity)
        order.tasks[task_id] = task
        self._task_to_order[task_id] = order.order_id

    def _replay(self, operation: str, key: str,
                fingerprint: Tuple[object, ...]) -> bool:
        receipt_key = (operation, _required(key, "idempotency key"))
        previous = self._receipts.get(receipt_key)
        if previous is not None and previous != fingerprint:
            raise IdempotencyConflict("idempotency key reused with new input")
        return previous is not None

    def _record(self, operation: str, key: str,
                fingerprint: Tuple[object, ...]) -> None:
        self._receipts[(operation, _required(key, "idempotency key"))] = fingerprint

    def _order(self, order_id: str) -> FulfilmentOrder:
        try:
            return self._orders[order_id]
        except KeyError as error:
            raise InvalidState("unknown order {}".format(order_id)) from error

    def _task(self, task_id: str) -> Tuple[FulfilmentOrder, PickTask]:
        try:
            order = self._orders[self._task_to_order[task_id]]
            return order, order.tasks[task_id]
        except KeyError as error:
            raise InvalidState("unknown task {}".format(task_id)) from error

    def _package(self, package_id: str) -> Tuple[FulfilmentOrder, Package]:
        try:
            order = self._orders[self._package_to_order[package_id]]
            return order, order.packages[package_id]
        except KeyError as error:
            raise InvalidState("unknown package {}".format(package_id)) from error

    @staticmethod
    def _line(order: FulfilmentOrder, line_id: str) -> FulfilmentLine:
        try:
            return order.lines[line_id]
        except KeyError as error:
            raise InvalidState("unknown line {}".format(line_id)) from error
