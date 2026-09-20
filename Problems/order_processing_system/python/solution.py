"""Interview-sized order core: domain rules, ports, replay, and compensation.

The per-order lock is the in-memory transaction boundary.  HTTP, persistence,
outbox processing, and reconciliation workers are deliberately left to discussion.
"""

from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime
from decimal import Decimal
from enum import Enum
from threading import RLock
from typing import Dict, Iterable, Mapping, Optional, Protocol, Tuple


class DomainError(Exception):
    pass


class IdempotencyConflict(DomainError):
    pass


@dataclass(frozen=True)
class Money:
    amount: Decimal
    currency: str

    def __post_init__(self) -> None:
        if not isinstance(self.amount, Decimal):
            raise TypeError("construct money from Decimal/string, never float")
        currency = self.currency.strip().upper()
        if not self.amount.is_finite() or len(currency) != 3:
            raise ValueError("money needs a finite amount and three-letter currency")
        object.__setattr__(self, "currency", currency)

    @classmethod
    def parse(cls, amount: str, currency: str) -> "Money":
        return cls(Decimal(amount), currency)

    @classmethod
    def zero(cls, currency: str) -> "Money":
        return cls(Decimal("0"), currency)

    def __add__(self, other: "Money") -> "Money":
        self._same_currency(other)
        return Money(self.amount + other.amount, self.currency)

    def __sub__(self, other: "Money") -> "Money":
        self._same_currency(other)
        return Money(self.amount - other.amount, self.currency)

    def times(self, quantity: int) -> "Money":
        return Money(self.amount * quantity, self.currency)

    def _same_currency(self, other: "Money") -> None:
        if self.currency != other.currency:
            raise ValueError("currency mismatch")


@dataclass(frozen=True)
class OrderLine:
    line_id: str
    sku: str
    product_name: str
    ordered: int
    unit_price: Money
    unit_discount: Money
    fulfilled: int = 0
    cancelled: int = 0

    def __post_init__(self) -> None:
        if not self.line_id.strip() or not self.sku.strip() or not self.product_name.strip():
            raise ValueError("line identifiers and product snapshot cannot be blank")
        if self.ordered <= 0 or self.unit_price.amount < 0:
            raise ValueError("quantity must be positive and price non-negative")
        self.unit_price._same_currency(self.unit_discount)
        if not Decimal("0") <= self.unit_discount.amount <= self.unit_price.amount:
            raise ValueError("discount must be between zero and price")
        if self.fulfilled < 0 or self.cancelled < 0 or self.open < 0:
            raise ValueError("line counters are inconsistent")

    @property
    def open(self) -> int:
        return self.ordered - self.fulfilled - self.cancelled

    @property
    def total(self) -> Money:
        return (self.unit_price - self.unit_discount).times(self.ordered)

    def apply(self, action: str, quantity: int) -> "OrderLine":
        if quantity <= 0 or quantity > self.open:
            raise DomainError("quantity must be within the open quantity")
        if action == "fulfil":
            return replace(self, fulfilled=self.fulfilled + quantity)
        if action == "cancel":
            return replace(self, cancelled=self.cancelled + quantity)
        raise ValueError("unknown line action")


class OrderStatus(Enum):
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    PARTIALLY_FULFILLED = "PARTIALLY_FULFILLED"
    FULFILLED = "FULFILLED"
    CANCELLED = "CANCELLED"


@dataclass(frozen=True)
class Order:
    order_id: str
    customer_id: str
    lines: Tuple[OrderLine, ...]
    created_at: datetime
    status: OrderStatus = OrderStatus.PENDING
    version: int = 1
    reservation_id: Optional[str] = None
    payment_id: Optional[str] = None

    @classmethod
    def create(
        cls,
        order_id: str,
        customer_id: str,
        lines: Tuple[OrderLine, ...],
        now: datetime,
    ) -> "Order":
        if not order_id.strip() or not customer_id.strip() or not lines:
            raise ValueError("order, customer, and at least one line are required")
        if len({line.line_id for line in lines}) != len(lines):
            raise ValueError("line IDs must be unique")
        if len({line.unit_price.currency for line in lines}) != 1:
            raise ValueError("an order must use one currency")
        return cls(order_id, customer_id, lines, now)

    @property
    def payable_total(self) -> Money:
        total = Money.zero(self.lines[0].unit_price.currency)
        for line in self.lines:
            total = total + line.total
        return total

    @property
    def open_sku_quantities(self) -> Mapping[str, int]:
        quantities: Dict[str, int] = {}
        for line in self.lines:
            quantities[line.sku] = quantities.get(line.sku, 0) + line.open
        return quantities

    def confirm(self, reservation_id: str, payment_id: str) -> "Order":
        if self.status is not OrderStatus.PENDING:
            raise DomainError("only a pending order can be confirmed")
        if not reservation_id or not payment_id:
            raise ValueError("reservation and payment IDs are required")
        return replace(
            self,
            status=OrderStatus.CONFIRMED,
            version=self.version + 1,
            reservation_id=reservation_id,
            payment_id=payment_id,
        )

    def change_line(self, action: str, line_id: str, quantity: int) -> "Order":
        if self.status not in (OrderStatus.CONFIRMED, OrderStatus.PARTIALLY_FULFILLED):
            raise DomainError("cannot {} from {}".format(action, self.status.value))
        changed = False
        lines = []
        for line in self.lines:
            if line.line_id == line_id:
                line = line.apply(action, quantity)
                changed = True
            lines.append(line)
        if not changed:
            raise DomainError("unknown line {}".format(line_id))
        fulfilled = sum(line.fulfilled for line in lines)
        open_quantity = sum(line.open for line in lines)
        if open_quantity == 0:
            status = OrderStatus.FULFILLED if fulfilled else OrderStatus.CANCELLED
        else:
            status = OrderStatus.PARTIALLY_FULFILLED if fulfilled else OrderStatus.CONFIRMED
        return replace(self, lines=tuple(lines), status=status, version=self.version + 1)


class Clock(Protocol):
    def now(self) -> datetime:
        ...


class RemoteStatus(Enum):
    SUCCEEDED = "SUCCEEDED"
    REJECTED = "REJECTED"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class RemoteResult:
    status: RemoteStatus
    reference: Optional[str] = None
    message: str = ""

    @classmethod
    def succeeded(cls, reference: str) -> "RemoteResult":
        return cls(RemoteStatus.SUCCEEDED, reference)

    @classmethod
    def rejected(cls, message: str) -> "RemoteResult":
        return cls(RemoteStatus.REJECTED, message=message)

    @classmethod
    def unknown(cls, message: str) -> "RemoteResult":
        return cls(RemoteStatus.UNKNOWN, message=message)


class InventoryPort(Protocol):
    def reserve(
        self, order_id: str, quantities: Mapping[str, int], key: str
    ) -> RemoteResult:
        ...

    def release(self, reservation_id: str, key: str) -> RemoteResult:
        ...


class PaymentPort(Protocol):
    def charge(self, order_id: str, amount: Money, key: str) -> RemoteResult:
        ...


class CommandOutcome(Enum):
    APPLIED = "APPLIED"
    REJECTED = "REJECTED"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class CommandResult:
    outcome: CommandOutcome
    order: Order
    message: str = ""


@dataclass(frozen=True)
class _Receipt:
    fingerprint: Tuple[object, ...]
    result: CommandResult


@dataclass
class _Confirmation:
    fingerprint: Tuple[object, ...]
    inventory_key: str
    payment_key: str
    release_key: str
    reservation_id: Optional[str] = None
    payment_id: Optional[str] = None
    payment_failure: Optional[str] = None


class OrderService:
    """Serializes commands per order and replays completed command receipts."""

    def __init__(self, clock: Clock, inventory: InventoryPort, payments: PaymentPort) -> None:
        self._clock, self._inventory, self._payments = clock, inventory, payments
        self._orders: Dict[str, Order] = {}
        self._receipts: Dict[Tuple[str, str], _Receipt] = {}
        self._confirmations: Dict[Tuple[str, str], _Confirmation] = {}
        self._active_confirmation: Dict[str, Tuple[str, str]] = {}
        self._order_locks: Dict[str, RLock] = {}
        self._command_locks: Dict[Tuple[str, str], RLock] = {}
        self._meta_lock = RLock()

    def create_order(
        self,
        idempotency_key: str,
        order_id: str,
        customer_id: str,
        lines: Iterable[OrderLine],
    ) -> CommandResult:
        line_tuple = tuple(lines)
        scope = self._scope("create", idempotency_key)
        fingerprint = (order_id, customer_id, line_tuple)
        with self._command_lock(scope), self._order_lock(order_id):
            replay = self._replay(scope, fingerprint)
            if replay:
                return replay
            existing = self._orders.get(order_id)
            if existing:
                result = CommandResult(CommandOutcome.REJECTED, existing, "order exists")
            else:
                order = Order.create(order_id, customer_id, line_tuple, self._clock.now())
                self._orders[order_id] = order
                result = CommandResult(CommandOutcome.APPLIED, order)
            return self._remember(scope, fingerprint, result)

    def confirm_order(self, idempotency_key: str, order_id: str) -> CommandResult:
        scope, fingerprint = self._scope("confirm", idempotency_key), (order_id,)
        with self._command_lock(scope), self._order_lock(order_id):
            replay = self._replay(scope, fingerprint)
            if replay:
                return replay
            order = self._require_order(order_id)
            progress = self._confirmations.get(scope)
            if progress is None:
                if order.status is not OrderStatus.PENDING:
                    return self._remember(
                        scope,
                        fingerprint,
                        CommandResult(CommandOutcome.REJECTED, order, "order is not pending"),
                    )
                owner = self._active_confirmation.get(order_id)
                if owner and owner != scope:
                    return CommandResult(
                        CommandOutcome.UNKNOWN, order, "another confirmation is unresolved"
                    )
                prefix = "order:{}:{}".format(order_id, idempotency_key)
                progress = _Confirmation(
                    fingerprint, prefix + ":inventory", prefix + ":payment", prefix + ":release"
                )
                self._confirmations[scope] = progress
                self._active_confirmation[order_id] = scope
            elif progress.fingerprint != fingerprint:
                raise IdempotencyConflict("key reused with different input")

            if progress.reservation_id is None:
                remote = self._inventory.reserve(
                    order_id, order.open_sku_quantities, progress.inventory_key
                )
                if remote.status is RemoteStatus.UNKNOWN:
                    return CommandResult(CommandOutcome.UNKNOWN, order, remote.message)
                if remote.status is RemoteStatus.REJECTED:
                    return self._finish(scope, progress, CommandResult(
                        CommandOutcome.REJECTED, order, "inventory rejected: " + remote.message
                    ))
                progress.reservation_id = self._reference(remote, "reservation")

            if progress.payment_id is None and progress.payment_failure is None:
                remote = self._payments.charge(
                    order_id, order.payable_total, progress.payment_key
                )
                if remote.status is RemoteStatus.UNKNOWN:
                    # A timeout may hide a successful charge: never release here.
                    return CommandResult(CommandOutcome.UNKNOWN, order, remote.message)
                if remote.status is RemoteStatus.REJECTED:
                    progress.payment_failure = remote.message or "payment rejected"
                else:
                    progress.payment_id = self._reference(remote, "payment")

            if progress.payment_failure is not None:
                remote = self._inventory.release(
                    progress.reservation_id, progress.release_key
                )
                if remote.status is not RemoteStatus.SUCCEEDED:
                    return CommandResult(
                        CommandOutcome.UNKNOWN, order, "inventory release is unresolved"
                    )
                return self._finish(scope, progress, CommandResult(
                    CommandOutcome.REJECTED, order,
                    "payment rejected; inventory released: " + progress.payment_failure,
                ))

            confirmed = order.confirm(progress.reservation_id, progress.payment_id)
            self._orders[order_id] = confirmed
            return self._finish(
                scope, progress, CommandResult(CommandOutcome.APPLIED, confirmed)
            )

    def fulfil_line(
        self, key: str, order_id: str, line_id: str, quantity: int
    ) -> CommandResult:
        return self._line_command("fulfil", key, order_id, line_id, quantity)

    def cancel_line(
        self, key: str, order_id: str, line_id: str, quantity: int
    ) -> CommandResult:
        return self._line_command("cancel", key, order_id, line_id, quantity)

    def _line_command(
        self, action: str, key: str, order_id: str, line_id: str, quantity: int
    ) -> CommandResult:
        scope = self._scope(action, key)
        fingerprint = (order_id, line_id, quantity)
        with self._command_lock(scope), self._order_lock(order_id):
            replay = self._replay(scope, fingerprint)
            if replay:
                return replay
            order = self._require_order(order_id)
            try:
                changed = order.change_line(action, line_id, quantity)
            except DomainError as error:
                result = CommandResult(CommandOutcome.REJECTED, order, str(error))
            else:
                self._orders[order_id] = changed
                result = CommandResult(CommandOutcome.APPLIED, changed)
            return self._remember(scope, fingerprint, result)

    def _finish(
        self, scope: Tuple[str, str], progress: _Confirmation, result: CommandResult
    ) -> CommandResult:
        self._remember(scope, progress.fingerprint, result)
        self._confirmations.pop(scope, None)
        if self._active_confirmation.get(result.order.order_id) == scope:
            self._active_confirmation.pop(result.order.order_id, None)
        return result

    def _replay(
        self, scope: Tuple[str, str], fingerprint: Tuple[object, ...]
    ) -> Optional[CommandResult]:
        receipt = self._receipts.get(scope)
        if receipt is None:
            return None
        if receipt.fingerprint != fingerprint:
            raise IdempotencyConflict("key reused with different input")
        return receipt.result

    def _remember(
        self, scope: Tuple[str, str], fingerprint: Tuple[object, ...], result: CommandResult
    ) -> CommandResult:
        self._receipts[scope] = _Receipt(fingerprint, result)
        return result

    def _require_order(self, order_id: str) -> Order:
        try:
            return self._orders[order_id]
        except KeyError:
            raise DomainError("unknown order {}".format(order_id))

    @staticmethod
    def _reference(result: RemoteResult, kind: str) -> str:
        if not result.reference:
            raise ValueError("successful {} result needs an ID".format(kind))
        return result.reference

    @staticmethod
    def _scope(operation: str, key: str) -> Tuple[str, str]:
        if not key or not key.strip():
            raise ValueError("idempotency key must not be blank")
        return operation, key

    def _order_lock(self, order_id: str) -> RLock:
        with self._meta_lock:
            return self._order_locks.setdefault(order_id, RLock())

    def _command_lock(self, scope: Tuple[str, str]) -> RLock:
        with self._meta_lock:
            return self._command_locks.setdefault(scope, RLock())
