from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from datetime import datetime, timezone
from decimal import Decimal
from enum import Enum, auto
from threading import RLock
from typing import Protocol
from uuid import UUID, uuid4

from .support import Money, RetryPolicy, async_retry


class Status(Enum):
    CREATED = auto()
    PROCESSING = auto()
    AUTHORIZED = auto()
    CAPTURED = auto()
    PARTIALLY_REFUNDED = auto()
    REFUNDED = auto()
    FAILED = auto()


class Operation(Enum):
    AUTHORIZE = auto()
    CAPTURE = auto()
    REFUND = auto()


@dataclass(frozen=True)
class GatewayRequest:
    payment_id: UUID
    operation: Operation
    amount: Money
    idempotency_key: str


@dataclass(frozen=True)
class GatewayResult:
    provider_reference: str
    status: Status


@dataclass(frozen=True)
class Transaction:
    operation: Operation
    amount: Money
    provider_reference: str
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


class Gateway(Protocol):
    async def execute(self, request: GatewayRequest) -> GatewayResult: ...


@dataclass
class Payment:
    id: UUID
    order_id: str
    amount: Money
    status: Status = Status.CREATED
    captured: int = 0
    refunded: int = 0
    ledger: list[Transaction] = field(default_factory=list)
    _lock: RLock = field(default_factory=RLock, repr=False)

    def begin(self, operation: Operation, amount: Money) -> None:
        with self._lock:
            allowed = {
                Operation.AUTHORIZE: {Status.CREATED},
                Operation.CAPTURE: {Status.AUTHORIZED},
                Operation.REFUND: {Status.CAPTURED, Status.PARTIALLY_REFUNDED},
            }
            if self.status not in allowed[operation]:
                raise RuntimeError(f"cannot {operation.name} from {self.status.name}")
            if operation is Operation.REFUND and self.refunded + amount.minor_units > self.captured:
                raise ValueError("refund exceeds captured amount")
            self.status = Status.PROCESSING

    def apply(self, operation: Operation, amount: Money, result: GatewayResult) -> None:
        with self._lock:
            if any(transaction.provider_reference == result.provider_reference
                   and transaction.operation is operation for transaction in self.ledger):
                return
            if result.status is Status.FAILED:
                self.status = Status.FAILED
                return
            self.ledger.append(Transaction(operation, amount, result.provider_reference))
            if operation is Operation.AUTHORIZE:
                self.status = Status.AUTHORIZED
            elif operation is Operation.CAPTURE:
                self.captured += amount.minor_units
                self.status = Status.CAPTURED
            else:
                self.refunded += amount.minor_units
                self.status = (Status.REFUNDED if self.refunded == self.captured
                               else Status.PARTIALLY_REFUNDED)

    def fail_if_processing(self) -> None:
        with self._lock:
            if self.status is Status.PROCESSING:
                self.status = Status.FAILED


class PaymentService:
    def __init__(self, gateway: Gateway) -> None:
        self._gateway = gateway
        self._payments: dict[UUID, Payment] = {}
        self._commands: dict[str, asyncio.Task[Payment]] = {}
        self._command_lock = asyncio.Lock()
        self._webhook_events: set[str] = set()
        self._webhook_lock = RLock()

    def create(self, order_id: str, amount: Money) -> Payment:
        payment = Payment(uuid4(), order_id, amount)
        self._payments[payment.id] = payment
        return payment

    async def execute(self, payment_id: UUID, operation: Operation, amount: Money,
                      idempotency_key: str) -> Payment:
        async with self._command_lock:
            task = self._commands.get(idempotency_key)
            if task is None:
                task = asyncio.create_task(
                    self._execute(payment_id, operation, amount, idempotency_key))
                self._commands[idempotency_key] = task
        return await asyncio.shield(task)

    async def _execute(self, payment_id: UUID, operation: Operation, amount: Money,
                       key: str) -> Payment:
        payment = self._payments[payment_id]
        payment.begin(operation, amount)
        request = GatewayRequest(payment_id, operation, amount, key)
        policy = RetryPolicy(3, Decimal("0.001"), Decimal("0.01"),
                             lambda error: isinstance(error, TimeoutError))
        try:
            result = await async_retry(lambda: self._gateway.execute(request), policy)
            payment.apply(operation, amount, result)
            return payment
        except Exception:
            payment.fail_if_processing()
            raise

    def accept_webhook(self, event_id: str, payment_id: UUID, operation: Operation,
                       amount: Money, result: GatewayResult) -> None:
        with self._webhook_lock:
            if event_id in self._webhook_events:
                return
            self._webhook_events.add(event_id)
        self._payments[payment_id].apply(operation, amount, result)


class _FlakyGateway:
    def __init__(self) -> None:
        self.calls = 0

    async def execute(self, request: GatewayRequest) -> GatewayResult:
        self.calls += 1
        if self.calls == 1:
            raise TimeoutError("unknown provider outcome")
        return GatewayResult("provider-auth-1", Status.AUTHORIZED)


async def _demo() -> None:
    gateway = _FlakyGateway()
    service = PaymentService(gateway)
    payment = service.create("order-1", Money(5_000, "USD"))
    first, duplicate = await asyncio.gather(
        service.execute(payment.id, Operation.AUTHORIZE, payment.amount, "order-1:authorize"),
        service.execute(payment.id, Operation.AUTHORIZE, payment.amount, "order-1:authorize"),
    )
    assert first is duplicate and payment.status is Status.AUTHORIZED and gateway.calls == 2
    service.accept_webhook("evt-1", payment.id, Operation.AUTHORIZE, payment.amount,
                           GatewayResult("provider-auth-1", Status.AUTHORIZED))
    assert len(payment.ledger) == 1


def run_demo() -> None:
    asyncio.run(_demo())
    print("Python Payment Processing: passed")
