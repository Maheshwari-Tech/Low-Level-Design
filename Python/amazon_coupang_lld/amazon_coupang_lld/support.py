from __future__ import annotations

import asyncio
import inspect
from collections import defaultdict
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from decimal import Decimal
from typing import Any, TypeVar

T = TypeVar("T")


@dataclass(frozen=True)
class Money:
    minor_units: int
    currency: str

    def __post_init__(self) -> None:
        if not self.currency:
            raise ValueError("currency is required")

    def __add__(self, other: Money) -> Money:
        self._require_same_currency(other)
        return Money(self.minor_units + other.minor_units, self.currency)

    def __sub__(self, other: Money) -> Money:
        self._require_same_currency(other)
        return Money(self.minor_units - other.minor_units, self.currency)

    def _require_same_currency(self, other: Money) -> None:
        if self.currency != other.currency:
            raise ValueError("currency mismatch")


@dataclass(frozen=True)
class RetryPolicy:
    max_attempts: int
    initial_delay_seconds: Decimal
    max_delay_seconds: Decimal
    retryable: Callable[[BaseException], bool]

    def __post_init__(self) -> None:
        if self.max_attempts < 1:
            raise ValueError("max_attempts must be positive")
        if self.initial_delay_seconds < 0 or self.max_delay_seconds < self.initial_delay_seconds:
            raise ValueError("invalid retry delay")

    def delay_before(self, next_attempt: int) -> float:
        multiplier = 2 ** max(0, next_attempt - 2)
        return float(min(self.max_delay_seconds, self.initial_delay_seconds * multiplier))


async def async_retry(operation: Callable[[], Awaitable[T]], policy: RetryPolicy) -> T:
    """Retry transient async work without blocking the event loop."""
    for attempt in range(1, policy.max_attempts + 1):
        try:
            return await operation()
        except Exception as error:
            if attempt == policy.max_attempts or not policy.retryable(error):
                raise
            await asyncio.sleep(policy.delay_before(attempt + 1))
    raise AssertionError("unreachable")


Handler = Callable[[Any], Any]


class EventBus:
    """In-process async Observer; it is deliberately not a durable broker."""

    def __init__(self) -> None:
        self._handlers: dict[type[Any], list[Handler]] = defaultdict(list)

    def subscribe(self, event_type: type[Any], handler: Handler) -> Callable[[], None]:
        self._handlers[event_type].append(handler)

        def unsubscribe() -> None:
            self._handlers[event_type].remove(handler)

        return unsubscribe

    async def publish(self, event: Any) -> None:
        async def invoke(handler: Handler) -> None:
            result = handler(event)
            if inspect.isawaitable(result):
                await result

        await asyncio.gather(*(invoke(handler) for handler in tuple(self._handlers[type(event)])))
