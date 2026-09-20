"""One-hour payment-processing core: exact money, retries, and reconciliation."""

from __future__ import annotations

from dataclasses import dataclass, replace
from decimal import Decimal
from enum import Enum
from threading import Event, RLock
from typing import Any, Callable, Dict, List, Optional, Protocol, Tuple
class PaymentError(Exception): pass
class InvalidState(PaymentError): pass
class AmountExceeded(PaymentError): pass
class OperationInProgress(PaymentError): pass
class IdempotencyConflict(PaymentError): pass
class CallbackError(PaymentError): pass
def _text(value: str, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("{} must not be blank".format(name))
    return value.strip()
@dataclass(frozen=True)
class Money:
    amount: Decimal
    currency: str

    def __post_init__(self) -> None:
        if not isinstance(self.amount, Decimal):
            raise TypeError("amount must be Decimal")
        if not self.amount.is_finite():
            raise ValueError("amount must be finite")
        currency = _text(self.currency, "currency").upper()
        if len(currency) != 3 or not currency.isalpha():
            raise ValueError("currency must be a three-letter code")
        object.__setattr__(self, "currency", currency)

    @classmethod
    def of(cls, amount: str, currency: str = "USD") -> "Money":
        return cls(Decimal(amount), currency)

    @classmethod
    def zero(cls, currency: str) -> "Money":
        return cls(Decimal("0"), currency)

    def positive(self) -> None:
        if self.amount <= 0:
            raise ValueError("money must be positive")

    def add(self, other: "Money") -> "Money":
        if self.currency != other.currency:
            raise ValueError("currency mismatch")
        return Money(self.amount + other.amount, self.currency)
class Operation(str, Enum):
    AUTHORIZE = "authorize"
    CAPTURE = "capture"
    VOID = "void"
    REFUND = "refund"

class ProviderOutcome(str, Enum):
    APPROVED = "approved"
    DECLINED = "declined"
    FAILED = "failed"  # Provider proves no money moved; a new command may retry.
    UNKNOWN = "unknown"  # Timeout/ambiguous transport; do not issue new work.

class AttemptStatus(str, Enum):
    PENDING = "pending"
    SUCCEEDED = "succeeded"
    DECLINED = "declined"
    FAILED = "failed"
    UNKNOWN = "unknown"

class PaymentStatus(str, Enum):
    CREATED = "created"
    AUTHORIZING = "authorizing"
    AUTHORIZED = "authorized"
    PARTIALLY_CAPTURED = "partially_captured"
    CAPTURED = "captured"
    VOIDED = "voided"
    DECLINED = "declined"
    FAILED = "failed"
    PARTIALLY_REFUNDED = "partially_refunded"
    REFUNDED = "refunded"

class CallbackDisposition(str, Enum):
    APPLIED = "applied"
    DUPLICATE = "duplicate"
    STALE = "stale"

@dataclass(frozen=True)
class ProviderResponse:
    outcome: ProviderOutcome
    reference: Optional[str] = None
    detail: str = ""

    def __post_init__(self) -> None:
        if self.reference is not None:
            object.__setattr__(self, "reference", _text(self.reference, "reference"))
        if self.outcome is ProviderOutcome.APPROVED and self.reference is None:
            raise ValueError("approved response requires a provider reference")

    @classmethod
    def approved(cls, reference: str) -> "ProviderResponse":
        return cls(ProviderOutcome.APPROVED, reference)

    @classmethod
    def unknown(cls, detail: str = "outcome unknown") -> "ProviderResponse":
        return cls(ProviderOutcome.UNKNOWN, detail=detail)

@dataclass(frozen=True)
class ProviderRequest:
    request_id: str
    payment_id: str
    order_id: str
    operation: Operation
    amount: Money
    method_token: str

@dataclass(frozen=True)
class ProviderCallback:
    callback_id: str
    provider_name: str
    request_id: str
    payment_id: str
    operation: Operation
    amount: Money
    response: ProviderResponse

class PaymentProvider(Protocol):
    name: str

    def execute(self, request: ProviderRequest) -> ProviderResponse: ...

@dataclass(frozen=True)
class Attempt:
    attempt_id: str
    request_id: str
    operation: Operation
    amount: Money
    status: AttemptStatus
    reference: Optional[str] = None
    detail: str = ""

@dataclass(frozen=True)
class PaymentSnapshot:
    payment_id: str
    order_id: str
    intent: Money
    captured: Money
    refunded: Money
    status: PaymentStatus
    pending_request_id: Optional[str]
    attempts: Tuple[Attempt, ...]

@dataclass(frozen=True)
class CommandResult:
    payment: PaymentSnapshot
    attempt: Optional[Attempt]

class _Payment:
    """The aggregate lock is a stand-in for a versioned database row."""

    def __init__(
        self,
        payment_id: str,
        order_id: str,
        intent: Money,
        method_token: str,
        provider_name: str,
    ) -> None:
        intent.positive()
        self.payment_id = _text(payment_id, "payment_id")
        self.order_id = _text(order_id, "order_id")
        self.intent = intent
        self.method_token = _text(method_token, "method_token")
        self.provider_name = _text(provider_name, "provider_name")
        self.captured = Money.zero(intent.currency)
        self.refunded = Money.zero(intent.currency)
        self.authorized = False
        self.voided = False
        self.status = PaymentStatus.CREATED
        self.pending: Optional[str] = None
        self.attempts: Dict[str, Attempt] = {}
        self.callbacks: Dict[str, Tuple[Any, ...]] = {}
        self.lock = RLock()

    def begin(self, attempt_id: str, request_id: str, op: Operation, amount: Money) -> None:
        with self.lock:
            if self.pending is not None:
                raise OperationInProgress("unresolved provider request {}".format(self.pending))
            self._validate(op, amount)
            self.attempts[request_id] = Attempt(
                attempt_id, request_id, op, amount, AttemptStatus.PENDING
            )
            self.pending = request_id
            if op is Operation.AUTHORIZE:
                self.status = PaymentStatus.AUTHORIZING

    def settle(self, request_id: str, response: ProviderResponse) -> Attempt:
        with self.lock:
            current = self.attempts.get(request_id)
            if current is None:
                raise CallbackError("unknown provider request")
            if current.status not in (AttemptStatus.PENDING, AttemptStatus.UNKNOWN):
                return current
            if response.outcome is ProviderOutcome.UNKNOWN:
                result = replace(
                    current,
                    status=AttemptStatus.UNKNOWN,
                    reference=response.reference,
                    detail=response.detail,
                )
                self.attempts[request_id] = result
                return result
            attempt_status = {
                ProviderOutcome.APPROVED: AttemptStatus.SUCCEEDED,
                ProviderOutcome.DECLINED: AttemptStatus.DECLINED,
                ProviderOutcome.FAILED: AttemptStatus.FAILED,
            }[response.outcome]
            result = replace(
                current,
                status=attempt_status,
                reference=response.reference,
                detail=response.detail,
            )
            self.attempts[request_id] = result
            self.pending = None
            if response.outcome is ProviderOutcome.APPROVED:
                self._apply(current.operation, current.amount)
            elif current.operation is Operation.AUTHORIZE:
                self.status = (
                    PaymentStatus.DECLINED
                    if response.outcome is ProviderOutcome.DECLINED
                    else PaymentStatus.FAILED
                )
            else:
                self.status = self._derived_status()
            return result

    def reconcile(self, callback: ProviderCallback) -> CallbackDisposition:
        with self.lock:
            callback_id = _text(callback.callback_id, "callback_id")
            attempt = self.attempts.get(callback.request_id)
            if callback.payment_id != self.payment_id or callback.provider_name != self.provider_name:
                raise CallbackError("callback route does not match payment")
            if attempt is None or attempt.operation is not callback.operation or attempt.amount != callback.amount:
                raise CallbackError("callback does not match attempt")
            fingerprint = (
                callback.provider_name,
                callback.request_id,
                callback.payment_id,
                callback.operation,
                callback.amount,
                callback.response,
            )
            prior = self.callbacks.get(callback_id)
            if prior is not None:
                if prior != fingerprint:
                    raise IdempotencyConflict("callback ID reused with different content")
                return CallbackDisposition.DUPLICATE
            self.callbacks[callback_id] = fingerprint
            if attempt.status not in (AttemptStatus.PENDING, AttemptStatus.UNKNOWN):
                return CallbackDisposition.STALE
            self.settle(callback.request_id, callback.response)
            return CallbackDisposition.APPLIED

    def snapshot(self) -> PaymentSnapshot:
        with self.lock:
            return PaymentSnapshot(
                self.payment_id,
                self.order_id,
                self.intent,
                self.captured,
                self.refunded,
                self.status,
                self.pending,
                tuple(self.attempts.values()),
            )

    def _validate(self, op: Operation, amount: Money) -> None:
        amount.positive()
        self.intent.add(Money.zero(amount.currency))  # Validates currency.
        if op is Operation.AUTHORIZE:
            if self.status not in (PaymentStatus.CREATED, PaymentStatus.FAILED):
                raise InvalidState("authorization is not allowed")
            if amount != self.intent:
                raise ValueError("authorization must equal intent")
        elif op is Operation.CAPTURE:
            if not self.authorized or self.voided:
                raise InvalidState("capture requires active authorization")
            if self.captured.add(amount).amount > self.intent.amount:
                raise AmountExceeded("capture exceeds authorization")
        elif op is Operation.VOID:
            if not self.authorized or self.voided or self.captured.amount:
                raise InvalidState("only an uncaptured authorization can be voided")
            if amount != self.intent:
                raise ValueError("void must target full authorization")
        elif op is Operation.REFUND:
            if not self.captured.amount:
                raise InvalidState("refund requires captured funds")
            if self.refunded.add(amount).amount > self.captured.amount:
                raise AmountExceeded("refund exceeds captured amount")

    def _apply(self, op: Operation, amount: Money) -> None:
        if op is Operation.AUTHORIZE:
            self.authorized = True
            self.status = PaymentStatus.AUTHORIZED
        elif op is Operation.CAPTURE:
            self.captured = self.captured.add(amount)
            self.status = self._derived_status()
        elif op is Operation.VOID:
            self.voided = True
            self.status = PaymentStatus.VOIDED
        else:
            self.refunded = self.refunded.add(amount)
            self.status = self._derived_status()

    def _derived_status(self) -> PaymentStatus:
        if self.voided:
            return PaymentStatus.VOIDED
        if self.refunded.amount:
            return (
                PaymentStatus.REFUNDED
                if self.refunded == self.captured
                else PaymentStatus.PARTIALLY_REFUNDED
            )
        if self.captured.amount:
            return (
                PaymentStatus.CAPTURED
                if self.captured == self.intent
                else PaymentStatus.PARTIALLY_CAPTURED
            )
        return PaymentStatus.AUTHORIZED if self.authorized else PaymentStatus.CREATED
@dataclass
class _IdempotencyEntry:
    fingerprint: Tuple[Any, ...]
    done: Event
    result: Any = None
    error: Optional[BaseException] = None
class PaymentService:
    def __init__(self, providers: List[PaymentProvider]) -> None:
        self.providers = {_text(provider.name, "provider name"): provider for provider in providers}
        if len(self.providers) != len(providers) or not providers:
            raise ValueError("provider names must be non-empty and unique")
        self.payments: Dict[str, _Payment] = {}
        self.routes: Dict[str, str] = {}
        self.idempotency: Dict[str, _IdempotencyEntry] = {}
        self.sequence = 0
        self.lock = RLock()

    def create(
        self,
        payment_id: str,
        order_id: str,
        amount: Money,
        method_token: str,
        provider_name: str,
        key: str,
    ) -> CommandResult:
        payment_id, order_id = _text(payment_id, "payment_id"), _text(order_id, "order_id")
        method_token, provider_name = _text(method_token, "method_token"), _text(provider_name, "provider_name")
        fingerprint = ("create", payment_id, order_id, amount, method_token, provider_name)

        def action() -> CommandResult:
            if provider_name not in self.providers:
                raise ValueError("unknown provider")
            payment = _Payment(payment_id, order_id, amount, method_token, provider_name)
            with self.lock:
                if payment_id in self.payments:
                    raise PaymentError("duplicate payment")
                self.payments[payment_id] = payment
            return CommandResult(payment.snapshot(), None)

        return self._once(key, fingerprint, action)

    def authorize(self, payment_id: str, key: str) -> CommandResult:
        payment = self._payment(payment_id)
        return self._command(payment, Operation.AUTHORIZE, payment.intent, key)

    def capture(self, payment_id: str, amount: Money, key: str) -> CommandResult:
        return self._command(self._payment(payment_id), Operation.CAPTURE, amount, key)

    def void(self, payment_id: str, key: str) -> CommandResult:
        payment = self._payment(payment_id)
        return self._command(payment, Operation.VOID, payment.intent, key)

    def refund(self, payment_id: str, amount: Money, key: str) -> CommandResult:
        return self._command(self._payment(payment_id), Operation.REFUND, amount, key)

    def callback(self, callback: ProviderCallback) -> Tuple[CallbackDisposition, PaymentSnapshot]:
        with self.lock:
            payment_id = self.routes.get(callback.request_id)
        if payment_id != callback.payment_id:
            raise CallbackError("callback has no matching route")
        payment = self._payment(payment_id)
        return payment.reconcile(callback), payment.snapshot()

    def get(self, payment_id: str) -> PaymentSnapshot:
        return self._payment(payment_id).snapshot()

    def _command(self, payment: _Payment, op: Operation, amount: Money, key: str) -> CommandResult:
        def action() -> CommandResult:
            with self.lock:
                self.sequence += 1
                attempt_id = "attempt-{}".format(self.sequence)
                request_id = "request-{}".format(self.sequence)
            payment.begin(attempt_id, request_id, op, amount)
            with self.lock:
                self.routes[request_id] = payment.payment_id
            request = ProviderRequest(
                request_id, payment.payment_id, payment.order_id, op, amount, payment.method_token
            )
            try:
                response = self.providers[payment.provider_name].execute(request)
            except Exception as error:  # No proof that the provider did not execute.
                response = ProviderResponse.unknown(type(error).__name__)
            attempt = payment.settle(request_id, response)
            return CommandResult(payment.snapshot(), attempt)

        return self._once(key, (op.value, payment.payment_id, amount), action)

    def _payment(self, payment_id: str) -> _Payment:
        with self.lock:
            payment = self.payments.get(_text(payment_id, "payment_id"))
        if payment is None:
            raise KeyError("unknown payment")
        return payment

    def _once(
        self, key: str, fingerprint: Tuple[Any, ...], action: Callable[[], Any]
    ) -> Any:
        key = _text(key, "idempotency key")
        with self.lock:
            entry = self.idempotency.get(key)
            owner = entry is None
            if owner:
                entry = _IdempotencyEntry(fingerprint, Event())
                self.idempotency[key] = entry
            elif entry.fingerprint != fingerprint:
                raise IdempotencyConflict("key reused for another command")
        assert entry is not None
        if owner:
            try:
                entry.result = action()
            except BaseException as error:
                entry.error = error
            finally:
                entry.done.set()
        else:
            entry.done.wait()
        if entry.error is not None:
            raise entry.error
        return entry.result
