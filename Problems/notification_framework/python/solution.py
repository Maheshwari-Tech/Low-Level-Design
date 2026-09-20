"""One-hour notification core with durable state and honest send outcomes."""

from __future__ import annotations

from dataclasses import dataclass, field, replace
from datetime import datetime, timedelta, timezone
from enum import Enum
from hashlib import sha256
import json
from string import Formatter
from threading import Lock, RLock
from typing import Callable, Optional, Protocol


class NotificationError(Exception):
    pass
class IdempotencyConflict(NotificationError):
    pass
class TemplateDataError(NotificationError):
    pass


class Channel(str, Enum):
    EMAIL = "EMAIL"
    SMS = "SMS"
    PUSH = "PUSH"


class DeliveryStatus(str, Enum):
    READY = "READY"
    SENDING = "SENDING"
    RETRY_WAIT = "RETRY_WAIT"
    UNKNOWN = "UNKNOWN"
    DELIVERED = "DELIVERED"
    FAILED = "FAILED"
    SUPPRESSED = "SUPPRESSED"


class OutcomeKind(str, Enum):
    DELIVERED = "DELIVERED"
    TRANSIENT_FAILURE = "TRANSIENT_FAILURE"
    PERMANENT_FAILURE = "PERMANENT_FAILURE"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class RenderedMessage:
    subject: str
    body: str
    template_version: int
    locale: str


@dataclass(frozen=True)
class NotificationTemplate:
    notification_type: str
    channel: Channel
    locale: str
    version: int
    subject: str
    body: str

    def __post_init__(self) -> None:
        if not self.notification_type.strip() or not self.locale.strip() or self.version <= 0:
            raise ValueError("template identity/version is invalid")

    def render(self, data: dict[str, str]) -> RenderedMessage:
        fields = {
            name.split(".", 1)[0].split("[", 1)[0]
            for text in (self.subject, self.body)
            for _, name, _, _ in Formatter().parse(text)
            if name
        }
        missing = fields - data.keys()
        if missing:
            raise TemplateDataError(f"missing template fields: {sorted(missing)}")
        try:
            return RenderedMessage(
                self.subject.format_map(data),
                self.body.format_map(data),
                self.version,
                self.locale,
            )
        except (KeyError, ValueError) as error:
            raise TemplateDataError(str(error)) from error


class TemplateResolver(Protocol):
    def resolve(
        self, notification_type: str, channel: Channel, locale: str
    ) -> NotificationTemplate: ...


@dataclass(frozen=True)
class NotificationRequest:
    notification_type: str
    recipient_ref: str
    channel: Channel
    destination: str
    locale: str
    template_data: tuple[tuple[str, str], ...]

    def __post_init__(self) -> None:
        if not all(
            (self.notification_type.strip(), self.recipient_ref.strip(), self.locale.strip())
        ):
            raise ValueError("notification type, recipient, and locale are required")
        if len({key for key, _ in self.template_data}) != len(self.template_data):
            raise ValueError("template-data keys must be unique")

    @property
    def data(self) -> dict[str, str]:
        return dict(self.template_data)

    def fingerprint(self) -> str:
        return _digest(
            (
                self.notification_type,
                self.recipient_ref,
                self.channel.value,
                self.destination,
                self.locale,
                sorted(self.template_data),
            )
        )


class PreferencePolicy(Protocol):
    def allows(self, request: NotificationRequest) -> tuple[bool, str]: ...


@dataclass(frozen=True)
class ProviderRequest:
    delivery_id: str
    provider_idempotency_key: str
    destination: str
    message: RenderedMessage


@dataclass(frozen=True)
class ProviderResult:
    kind: OutcomeKind
    error_code: Optional[str] = None
    retry_after: Optional[timedelta] = None

    @classmethod
    def delivered(cls) -> ProviderResult:
        return cls(OutcomeKind.DELIVERED)

    @classmethod
    def transient(
        cls, code: str, retry_after: Optional[timedelta] = None
    ) -> ProviderResult:
        return cls(OutcomeKind.TRANSIENT_FAILURE, code, retry_after)

    @classmethod
    def permanent(cls, code: str) -> ProviderResult:
        return cls(OutcomeKind.PERMANENT_FAILURE, code)

    @classmethod
    def unknown(cls, code: str = "AMBIGUOUS") -> ProviderResult:
        return cls(OutcomeKind.UNKNOWN, code)


class ChannelProvider(Protocol):
    name: str
    supports_idempotency: bool
    supports_lookup: bool

    def send(self, request: ProviderRequest) -> ProviderResult: ...
    def lookup(
        self, provider_idempotency_key: str
    ) -> Optional[ProviderResult]: ...


@dataclass(frozen=True)
class RetryPolicy:
    max_attempts: int = 2
    base_delay: timedelta = timedelta(seconds=1)
    max_delay: timedelta = timedelta(minutes=1)

    def __post_init__(self) -> None:
        if self.max_attempts <= 0:
            raise ValueError("max_attempts must be positive")
        if self.base_delay <= timedelta(0) or self.max_delay < self.base_delay:
            raise ValueError("retry delays are invalid")

    def delay(
        self, attempts: int, provider_hint: Optional[timedelta]
    ) -> timedelta:
        calculated = min(self.base_delay * (2 ** max(0, attempts - 1)), self.max_delay)
        return max(calculated, provider_hint or timedelta(0))


class ManualClock:
    def __init__(self, initial: datetime) -> None:
        if initial.tzinfo is None:
            raise ValueError("clock must be timezone-aware")
        self._value = initial
        self._lock = Lock()

    def now(self) -> datetime:
        with self._lock:
            return self._value

    def advance(self, duration: timedelta) -> None:
        if duration < timedelta(0):
            raise ValueError("clock cannot move backwards")
        with self._lock:
            self._value += duration


@dataclass(frozen=True)
class DeliveryAttempt:
    ordinal: int
    provider_idempotency_key: str
    started_at: datetime
    finished_at: Optional[datetime] = None
    outcome: Optional[OutcomeKind] = None
    error_code: Optional[str] = None


@dataclass(frozen=True)
class DeliveryView:
    delivery_id: str
    status: DeliveryStatus
    suppression_reason: Optional[str]
    rendered: Optional[RenderedMessage]
    next_attempt_at: Optional[datetime]
    attempts: tuple[DeliveryAttempt, ...]


@dataclass(frozen=True)
class SubmissionResult:
    delivery_id: str
    status: DeliveryStatus
    replayed: bool = False


@dataclass
class _Delivery:
    delivery_id: str
    request: NotificationRequest
    rendered: Optional[RenderedMessage]
    status: DeliveryStatus
    suppression_reason: Optional[str] = None
    next_attempt_at: Optional[datetime] = None
    attempts: list[DeliveryAttempt] = field(default_factory=list)


class NotificationService:
    """The lock models transactional submit, claim, and completion transitions."""

    def __init__(
        self,
        templates: TemplateResolver,
        preferences: PreferencePolicy,
        providers: dict[Channel, ChannelProvider],
        *,
        retry_policy: RetryPolicy = RetryPolicy(),
        clock: Optional[Callable[[], datetime]] = None,
    ) -> None:
        self._templates = templates
        self._preferences = preferences
        self._providers = dict(providers)
        self._retry = retry_policy
        self._clock = clock or (lambda: datetime.now(timezone.utc))
        if self._clock().tzinfo is None:
            raise ValueError("clock must return timezone-aware datetimes")
        self._deliveries: dict[str, _Delivery] = {}
        self._idempotency: dict[str, tuple[str, str]] = {}
        self._sequence = 0
        self._lock = RLock()

    def submit(self, request: NotificationRequest, idempotency_key: str) -> SubmissionResult:
        if not idempotency_key.strip():
            raise ValueError("idempotency key is required")
        request_hash = request.fingerprint()
        with self._lock:
            existing = self._idempotency.get(idempotency_key)
            if existing is not None:
                stored_hash, delivery_id = existing
                if stored_hash != request_hash:
                    raise IdempotencyConflict("key reused for another request")
                return SubmissionResult(
                    delivery_id, self._deliveries[delivery_id].status, replayed=True
                )
            allowed, reason = self._preferences.allows(request)
            rendered = None
            status = DeliveryStatus.SUPPRESSED
            if allowed and request.destination.strip():
                template = self._templates.resolve(
                    request.notification_type, request.channel, request.locale
                )
                rendered = template.render(request.data)
                status = DeliveryStatus.READY
                reason = "ELIGIBLE"
            elif allowed:
                reason = "MISSING_DESTINATION"
            self._sequence += 1
            delivery_id = f"delivery-{self._sequence:04d}"
            delivery = _Delivery(
                delivery_id,
                request,
                rendered,
                status,
                None if status is DeliveryStatus.READY else reason,
            )
            self._deliveries[delivery_id] = delivery
            self._idempotency[idempotency_key] = (request_hash, delivery_id)
            return SubmissionResult(delivery_id, status)

    def process_due(self) -> int:
        now = self._clock()
        with self._lock:
            candidates = [
                delivery_id
                for delivery_id, delivery in self._deliveries.items()
                if delivery.status in {DeliveryStatus.READY, DeliveryStatus.RETRY_WAIT}
                and (delivery.next_attempt_at is None or delivery.next_attempt_at <= now)
            ]
        processed = 0
        for delivery_id in candidates:
            claim = self._claim(delivery_id, now)
            if claim is None:
                continue
            provider, request, ordinal = claim
            try:
                result = provider.send(request)
            except Exception as error:
                result = ProviderResult.transient(type(error).__name__)
            self._complete(delivery_id, ordinal, result)
            processed += 1
        return processed

    def reconcile_unknown(self, delivery_id: str) -> bool:
        with self._lock:
            delivery = self._deliveries[delivery_id]
            if delivery.status is not DeliveryStatus.UNKNOWN:
                return False
            provider = self._providers[delivery.request.channel]
            provider_key = delivery.attempts[-1].provider_idempotency_key
        if provider.supports_lookup:
            result = provider.lookup(provider_key)
            if result is None or result.kind is OutcomeKind.UNKNOWN:
                return False
            with self._lock:
                if delivery.status is not DeliveryStatus.UNKNOWN:
                    return False
                self._apply_result(delivery, result, self._clock())
                return True
        if provider.supports_idempotency:
            with self._lock:
                if delivery.status is not DeliveryStatus.UNKNOWN:
                    return False
                # Retrying the same provider key is duplicate-safe, but it still
                # consumes an attempt and must respect the configured bound.
                if len(delivery.attempts) >= self._retry.max_attempts:
                    return False
                delivery.status = DeliveryStatus.RETRY_WAIT
                delivery.next_attempt_at = self._clock()
                return True
        return False

    def view(self, delivery_id: str) -> DeliveryView:
        with self._lock:
            delivery = self._deliveries[delivery_id]
            return DeliveryView(
                delivery.delivery_id,
                delivery.status,
                delivery.suppression_reason,
                delivery.rendered,
                delivery.next_attempt_at,
                tuple(delivery.attempts),
            )

    def _claim(
        self, delivery_id: str, now: datetime
    ) -> Optional[tuple[ChannelProvider, ProviderRequest, int]]:
        with self._lock:
            delivery = self._deliveries[delivery_id]
            if delivery.status not in {DeliveryStatus.READY, DeliveryStatus.RETRY_WAIT}:
                return None
            if delivery.next_attempt_at is not None and delivery.next_attempt_at > now:
                return None
            provider = self._providers.get(delivery.request.channel)
            if provider is None:
                delivery.status = DeliveryStatus.FAILED
                return None
            ordinal = len(delivery.attempts) + 1
            provider_key = f"{delivery.delivery_id}/{provider.name}"
            delivery.attempts.append(DeliveryAttempt(ordinal, provider_key, now))
            delivery.status = DeliveryStatus.SENDING
            delivery.next_attempt_at = None
            return provider, ProviderRequest(
                delivery.delivery_id,
                provider_key,
                delivery.request.destination,
                delivery.rendered,
            ), ordinal

    def _complete(
        self, delivery_id: str, ordinal: int, result: ProviderResult
    ) -> None:
        now = self._clock()
        with self._lock:
            delivery = self._deliveries[delivery_id]
            if (
                delivery.status is not DeliveryStatus.SENDING
                or not delivery.attempts
                or delivery.attempts[-1].ordinal != ordinal
            ):
                return
            delivery.attempts[-1] = replace(
                delivery.attempts[-1],
                finished_at=now,
                outcome=result.kind,
                error_code=result.error_code,
            )
            self._apply_result(delivery, result, now)

    def _apply_result(
        self, delivery: _Delivery, result: ProviderResult, now: datetime
    ) -> None:
        if result.kind is OutcomeKind.DELIVERED:
            delivery.status = DeliveryStatus.DELIVERED
        elif result.kind is OutcomeKind.PERMANENT_FAILURE:
            delivery.status = DeliveryStatus.FAILED
        elif result.kind is OutcomeKind.UNKNOWN:
            delivery.status = DeliveryStatus.UNKNOWN
        elif len(delivery.attempts) >= self._retry.max_attempts:
            delivery.status = DeliveryStatus.FAILED
        else:
            delivery.status = DeliveryStatus.RETRY_WAIT
            delivery.next_attempt_at = now + self._retry.delay(
                len(delivery.attempts), result.retry_after
            )


def _digest(value: object) -> str:
    return sha256(
        json.dumps(value, sort_keys=True, separators=(",", ":"), default=str).encode()
    ).hexdigest()
