from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum, auto
from queue import Empty, Queue
from threading import Lock, RLock
from typing import Protocol
from uuid import UUID, uuid4

from .support import RetryPolicy, async_retry


class FulfilmentStatus(Enum):
    ALLOCATED = auto()
    PICKING = auto()
    PICKED = auto()
    PACKED = auto()
    SHIPPED = auto()


class PickStatus(Enum):
    AVAILABLE = auto()
    CLAIMED = auto()
    PICKED = auto()
    EXCEPTION = auto()


@dataclass(frozen=True)
class Line:
    sku: str
    quantity: int


@dataclass(frozen=True)
class Warehouse:
    id: str
    available_by_sku: dict[str, int]
    distance_km: int


class AllocationStrategy(Protocol):
    def choose(self, lines: tuple[Line, ...], candidates: tuple[Warehouse, ...]) -> Warehouse: ...


class NearestCompleteWarehouse:
    def choose(self, lines: tuple[Line, ...], candidates: tuple[Warehouse, ...]) -> Warehouse:
        eligible = [warehouse for warehouse in candidates
                    if all(warehouse.available_by_sku.get(line.sku, 0) >= line.quantity
                           for line in lines)]
        if not eligible:
            raise RuntimeError("no complete allocation")
        return min(eligible, key=lambda warehouse: (warehouse.distance_km, warehouse.id))


@dataclass
class PickTask:
    sku: str
    quantity: int
    id: UUID = field(default_factory=uuid4)
    status: PickStatus = PickStatus.AVAILABLE
    picker_id: str | None = None
    _lock: Lock = field(default_factory=Lock, repr=False)

    def claim(self, picker_id: str) -> bool:
        with self._lock:
            if self.status is not PickStatus.AVAILABLE:
                return False
            self.status = PickStatus.CLAIMED
            self.picker_id = picker_id
            return True

    def confirm(self, picker_id: str) -> bool:
        with self._lock:
            if self.status is not PickStatus.CLAIMED or self.picker_id != picker_id:
                return False
            self.status = PickStatus.PICKED
            return True


@dataclass
class Fulfilment:
    order_id: str
    warehouse: Warehouse
    tasks: tuple[PickTask, ...]
    status: FulfilmentStatus = FulfilmentStatus.ALLOCATED
    tracking_number: str | None = None
    _lock: RLock = field(default_factory=RLock, repr=False)

    def release_to_pick(self) -> None:
        with self._lock:
            self._require(FulfilmentStatus.ALLOCATED)
            self.status = FulfilmentStatus.PICKING

    def refresh(self) -> None:
        with self._lock:
            if all(task.status is PickStatus.PICKED for task in self.tasks):
                self.status = FulfilmentStatus.PICKED

    def pack(self) -> None:
        with self._lock:
            self._require(FulfilmentStatus.PICKED)
            self.status = FulfilmentStatus.PACKED

    def ship(self, tracking_number: str) -> None:
        with self._lock:
            self._require(FulfilmentStatus.PACKED)
            self.tracking_number = tracking_number
            self.status = FulfilmentStatus.SHIPPED

    def _require(self, expected: FulfilmentStatus) -> None:
        if self.status is not expected:
            raise RuntimeError(f"expected {expected.name}, got {self.status.name}")


class Carrier(Protocol):
    async def create_shipment(self, fulfilment: Fulfilment) -> str: ...


class WarehouseService:
    def __init__(self, allocator: AllocationStrategy, carrier: Carrier) -> None:
        self._allocator = allocator
        self._carrier = carrier
        self._work: Queue[PickTask] = Queue()
        self._owners: dict[UUID, Fulfilment] = {}

    def allocate(self, order_id: str, lines: tuple[Line, ...],
                 candidates: tuple[Warehouse, ...]) -> Fulfilment:
        warehouse = self._allocator.choose(lines, candidates)
        return Fulfilment(order_id, warehouse,
                          tuple(PickTask(line.sku, line.quantity) for line in lines))

    def release_to_pick(self, fulfilment: Fulfilment) -> None:
        fulfilment.release_to_pick()
        for task in fulfilment.tasks:
            self._owners[task.id] = fulfilment
            self._work.put(task)

    def claim_next(self, picker_id: str, timeout_seconds: float) -> PickTask | None:
        try:
            while task := self._work.get(timeout=timeout_seconds):
                if task.claim(picker_id):
                    return task
        except Empty:
            return None
        return None

    def confirm_picked(self, task: PickTask, picker_id: str) -> None:
        if not task.confirm(picker_id):
            raise RuntimeError("duplicate or wrong-picker scan")
        self._owners[task.id].refresh()

    async def ship(self, fulfilment: Fulfilment) -> str:
        fulfilment.pack()
        policy = RetryPolicy(3, Decimal("0.001"), Decimal("0.01"),
                             lambda error: isinstance(error, TimeoutError))
        tracking = await async_retry(lambda: self._carrier.create_shipment(fulfilment), policy)
        fulfilment.ship(tracking)
        return tracking


class _Carrier:
    async def create_shipment(self, fulfilment: Fulfilment) -> str:
        return "TRACK-1"


async def _demo() -> None:
    service = WarehouseService(NearestCompleteWarehouse(), _Carrier())
    lines = (Line("A", 2), Line("B", 1))
    fulfilment = service.allocate("order-1", lines, (
        Warehouse("far", {"A": 5, "B": 5}, 20),
        Warehouse("near", {"A": 5, "B": 5}, 5),
    ))
    service.release_to_pick(fulfilment)
    for _ in lines:
        task = service.claim_next("picker-1", 0.1)
        assert task is not None
        service.confirm_picked(task, "picker-1")
    await service.ship(fulfilment)
    assert fulfilment.status is FulfilmentStatus.SHIPPED and fulfilment.warehouse.id == "near"


def run_demo() -> None:
    asyncio.run(_demo())
    print("Python Warehouse Fulfilment: passed")
