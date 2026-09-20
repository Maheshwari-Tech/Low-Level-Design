from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from datetime import datetime, timezone
from decimal import Decimal
from enum import Enum, auto
from typing import Protocol
from uuid import UUID, uuid4

from .support import EventBus, Money, RetryPolicy, async_retry


class OrderStatus(Enum):
    DRAFT = auto()
    CHECKOUT_IN_PROGRESS = auto()
    CONFIRMED = auto()
    CANCELLED = auto()


@dataclass(frozen=True)
class OrderLine:
    sku: str
    quantity: int
    unit_price_snapshot: Money

    def __post_init__(self) -> None:
        if self.quantity <= 0:
            raise ValueError("quantity must be positive")

    @property
    def subtotal(self) -> Money:
        return Money(self.unit_price_snapshot.minor_units * self.quantity,
                     self.unit_price_snapshot.currency)


@dataclass
class Order:
    id: UUID
    lines: tuple[OrderLine, ...]
    status: OrderStatus = OrderStatus.DRAFT
    reservation_id: str | None = None
    payment_id: str | None = None

    @property
    def total(self) -> Money:
        if not self.lines:
            raise ValueError("empty order")
        result = Money(0, self.lines[0].unit_price_snapshot.currency)
        for line in self.lines:
            result += line.subtotal
        return result

    def begin_checkout(self) -> None:
        self._require(OrderStatus.DRAFT)
        self.status = OrderStatus.CHECKOUT_IN_PROGRESS

    def attach_reservation(self, reservation_id: str) -> None:
        self._require(OrderStatus.CHECKOUT_IN_PROGRESS)
        self.reservation_id = reservation_id

    def confirm(self, payment_id: str) -> None:
        self._require(OrderStatus.CHECKOUT_IN_PROGRESS)
        if self.reservation_id is None:
            raise RuntimeError("inventory is not reserved")
        self.payment_id = payment_id
        self.status = OrderStatus.CONFIRMED

    def cancel(self) -> None:
        if self.status is OrderStatus.CONFIRMED:
            raise RuntimeError("confirmed order requires refund workflow")
        self.status = OrderStatus.CANCELLED

    def _require(self, expected: OrderStatus) -> None:
        if self.status is not expected:
            raise RuntimeError(f"expected {expected.name}, got {self.status.name}")


class InventoryPort(Protocol):
    async def reserve(self, order_id: UUID, lines: tuple[OrderLine, ...]) -> str: ...
    async def release(self, reservation_id: str) -> None: ...


class PaymentPort(Protocol):
    async def authorize(self, order_id: UUID, amount: Money, idempotency_key: str) -> str: ...


@dataclass(frozen=True)
class OrderConfirmed:
    order_id: UUID
    occurred_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


class OrderService:
    def __init__(self, inventory: InventoryPort, payment: PaymentPort, events: EventBus) -> None:
        self._inventory = inventory
        self._payment = payment
        self._events = events
        self._orders: dict[UUID, Order] = {}
        self._commands: dict[str, asyncio.Task[Order]] = {}
        self._command_lock = asyncio.Lock()

    def add(self, order: Order) -> None:
        self._orders[order.id] = order

    async def checkout(self, order_id: UUID, idempotency_key: str) -> Order:
        async with self._command_lock:
            task = self._commands.get(idempotency_key)
            if task is None:
                task = asyncio.create_task(self._execute(order_id, idempotency_key))
                self._commands[idempotency_key] = task
        return await asyncio.shield(task)

    async def _execute(self, order_id: UUID, key: str) -> Order:
        order = self._orders[order_id]
        order.begin_checkout()
        reservation_id = await self._inventory.reserve(order.id, order.lines)
        order.attach_reservation(reservation_id)
        policy = RetryPolicy(3, Decimal("0.001"), Decimal("0.01"),
                             lambda error: isinstance(error, TimeoutError))
        try:
            payment_id = await async_retry(
                lambda: self._payment.authorize(order.id, order.total, key), policy)
        except Exception:
            await self._inventory.release(reservation_id)
            order.cancel()
            raise
        order.confirm(payment_id)
        await self._events.publish(OrderConfirmed(order.id))
        return order


class _Inventory:
    async def reserve(self, order_id: UUID, lines: tuple[OrderLine, ...]) -> str:
        return f"reservation-{order_id}"

    async def release(self, reservation_id: str) -> None:
        return None


class _Payment:
    async def authorize(self, order_id: UUID, amount: Money, idempotency_key: str) -> str:
        return f"payment-{order_id}"


async def _demo() -> None:
    events = EventBus()
    observed: list[OrderConfirmed] = []
    events.subscribe(OrderConfirmed, observed.append)
    service = OrderService(_Inventory(), _Payment(), events)
    order = Order(uuid4(), (OrderLine("SKU-1", 2, Money(1_500, "USD")),))
    service.add(order)
    first, duplicate = await asyncio.gather(
        service.checkout(order.id, "checkout-1"),
        service.checkout(order.id, "checkout-1"),
    )
    assert first is duplicate and order.status is OrderStatus.CONFIRMED
    assert len(observed) == 1 and order.total == Money(3_000, "USD")


def run_demo() -> None:
    asyncio.run(_demo())
    print("Python Order Management: passed")
