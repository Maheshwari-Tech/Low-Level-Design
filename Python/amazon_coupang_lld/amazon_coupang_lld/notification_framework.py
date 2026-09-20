from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from datetime import datetime, timezone
from decimal import Decimal
from enum import Enum, auto
from typing import Protocol
from uuid import UUID, uuid4

from .support import EventBus, RetryPolicy, async_retry


class Channel(Enum):
    EMAIL = auto()
    SMS = auto()
    PUSH = auto()


class Status(Enum):
    QUEUED = auto()
    SENDING = auto()
    ACCEPTED = auto()
    DELIVERED = auto()
    FAILED = auto()
    SUPPRESSED = auto()


@dataclass(frozen=True)
class Request:
    idempotency_key: str
    recipient: str
    channel: Channel
    template: str
    variables: dict[str, str]


@dataclass
class Notification:
    id: UUID
    request: Request
    rendered: str
    status: Status
    provider_id: str | None = None


@dataclass(frozen=True)
class StatusChanged:
    notification_id: UUID
    status: Status
    occurred_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


class Provider(Protocol):
    async def send(self, recipient: str, body: str, idempotency_key: str) -> str: ...


class NotificationService:
    """Bounded asyncio producer-consumer with async provider retry and dedupe."""

    _STOP = object()

    def __init__(self, providers: dict[Channel, Provider], events: EventBus,
                 *, capacity: int = 100, workers: int = 2) -> None:
        self._providers = providers
        self._events = events
        self._queue: asyncio.Queue[Notification | object] = asyncio.Queue(capacity)
        self._worker_count = workers
        self._workers: list[asyncio.Task[None]] = []
        self._outcomes: dict[str, asyncio.Future[Notification]] = {}
        self._notifications: dict[UUID, Notification] = {}
        self._lock = asyncio.Lock()

    async def start(self) -> None:
        self._workers = [asyncio.create_task(self._worker()) for _ in range(self._worker_count)]

    async def submit(self, request: Request) -> Notification:
        notification: Notification | None = None
        async with self._lock:
            outcome = self._outcomes.get(request.idempotency_key)
            if outcome is None:
                outcome = asyncio.get_running_loop().create_future()
                self._outcomes[request.idempotency_key] = outcome
                rendered = request.template.format_map(request.variables)
                notification = Notification(uuid4(), request, rendered, Status.QUEUED)
                self._notifications[notification.id] = notification
        if notification is not None:
            await self._events.publish(StatusChanged(notification.id, Status.QUEUED))
            try:
                self._queue.put_nowait(notification)
            except asyncio.QueueFull as error:
                async with self._lock:
                    self._outcomes.pop(request.idempotency_key, None)
                outcome.set_exception(RuntimeError("notification queue full"))
                raise RuntimeError("notification queue full") from error
        return await asyncio.shield(outcome)

    async def mark_delivered(self, notification_id: UUID) -> None:
        notification = self._notifications[notification_id]
        if notification.status is not Status.DELIVERED:
            notification.status = Status.DELIVERED
            await self._events.publish(StatusChanged(notification.id, Status.DELIVERED))

    async def _worker(self) -> None:
        policy = RetryPolicy(3, Decimal("0.001"), Decimal("0.01"),
                             lambda error: isinstance(error, TimeoutError))
        while True:
            item = await self._queue.get()
            try:
                if item is self._STOP:
                    return
                notification = item
                assert isinstance(notification, Notification)
                notification.status = Status.SENDING
                await self._events.publish(StatusChanged(notification.id, Status.SENDING))
                try:
                    notification.provider_id = await async_retry(
                        lambda: self._providers[notification.request.channel].send(
                            notification.request.recipient, notification.rendered,
                            notification.request.idempotency_key), policy)
                    notification.status = Status.ACCEPTED
                    self._outcomes[notification.request.idempotency_key].set_result(notification)
                except Exception as error:
                    notification.status = Status.FAILED
                    self._outcomes[notification.request.idempotency_key].set_exception(error)
                await self._events.publish(StatusChanged(notification.id, notification.status))
            finally:
                self._queue.task_done()

    async def close(self) -> None:
        await self._queue.join()
        for _ in self._workers:
            await self._queue.put(self._STOP)
        await asyncio.gather(*self._workers)


class _FlakyProvider:
    def __init__(self) -> None:
        self.calls = 0

    async def send(self, recipient: str, body: str, idempotency_key: str) -> str:
        self.calls += 1
        if self.calls == 1:
            raise TimeoutError("unknown provider outcome")
        return "provider-1"


async def _demo() -> None:
    events = EventBus()
    observed: list[StatusChanged] = []
    events.subscribe(StatusChanged, observed.append)
    provider = _FlakyProvider()
    service = NotificationService({Channel.EMAIL: provider}, events)
    await service.start()
    request = Request("order-1:confirmed", "ada@example.com", Channel.EMAIL,
                      "Hi {name}", {"name": "Ada"})
    first, duplicate = await asyncio.gather(service.submit(request), service.submit(request))
    await service.close()
    assert first is duplicate and first.status is Status.ACCEPTED and provider.calls == 2
    assert sum(event.status is Status.ACCEPTED for event in observed) == 1


def run_demo() -> None:
    asyncio.run(_demo())
    print("Python Notification Framework: passed")
