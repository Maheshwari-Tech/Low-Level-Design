from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from threading import Barrier, Lock
import unittest
from solution import (
    Channel,
    DeliveryStatus,
    IdempotencyConflict,
    ManualClock,
    NotificationRequest,
    NotificationService,
    NotificationTemplate,
    ProviderResult,
    RetryPolicy,
)
NOW = datetime(2026, 8, 11, 10, tzinfo=timezone.utc)
def request(order: str = "O-42") -> NotificationRequest:
    return NotificationRequest(
        "ORDER_DELAYED",
        "user-7",
        Channel.PUSH,
        "push-token-7",
        "en-IN",
        (("order_id", order),),
    )

class Templates:
    def __init__(self) -> None:
        self.current = NotificationTemplate(
            "ORDER_DELAYED", Channel.PUSH, "en-IN", 1,
            "Order {order_id}", "Order {order_id} is delayed",
        )

    def resolve(self, kind, channel, locale):
        if (kind, channel, locale) != (
            self.current.notification_type, self.current.channel, self.current.locale
        ):
            raise KeyError((kind, channel, locale))
        return self.current

class Preferences:
    def __init__(self, allowed: bool = True) -> None:
        self.allowed = allowed

    def allows(self, notification):
        del notification
        return (True, "ELIGIBLE") if self.allowed else (False, "OPTED_OUT")


class Provider:
    name = "push-a"

    def __init__(
        self, outcomes: list[ProviderResult], *, lookup: bool = False,
        idempotency: bool = False,
    ) -> None:
        self.supports_lookup = lookup
        self.supports_idempotency = idempotency
        self.outcomes = outcomes
        self.calls = []
        self.lookup_result = None
        self.lock = Lock()

    def send(self, provider_request):
        with self.lock:
            self.calls.append(provider_request)
            return self.outcomes.pop(0)

    def lookup(self, provider_key):
        del provider_key
        return self.lookup_result


def service(provider: Provider, *, clock=lambda: NOW, preferences=None, retry=None):
    return NotificationService(
        Templates(),
        preferences or Preferences(),
        {Channel.PUSH: provider},
        clock=clock,
        retry_policy=retry or RetryPolicy(),
    )


class NotificationServiceTests(unittest.TestCase):
    def test_render_is_snapshotted_and_opt_out_never_calls_provider(self) -> None:
        templates = Templates()
        preferences = Preferences()
        provider = Provider([ProviderResult.delivered()])
        subject = NotificationService(
            templates, preferences, {Channel.PUSH: provider}, clock=lambda: NOW
        )
        accepted = subject.submit(request(), "key-1")
        templates.current = NotificationTemplate(
            "ORDER_DELAYED", Channel.PUSH, "en-IN", 2, "Changed", "Changed"
        )
        subject.process_due()
        self.assertEqual(provider.calls[0].message.template_version, 1)
        self.assertEqual(subject.view(accepted.delivery_id).status, DeliveryStatus.DELIVERED)

        preferences.allowed = False
        suppressed = subject.submit(request("O-43"), "key-2")
        self.assertEqual(suppressed.status, DeliveryStatus.SUPPRESSED)
        self.assertEqual(subject.view(suppressed.delivery_id).suppression_reason, "OPTED_OUT")
        self.assertEqual(len(provider.calls), 1)

    def test_submit_replays_same_request_and_rejects_key_reuse(self) -> None:
        subject = service(Provider([ProviderResult.delivered()]))
        first = subject.submit(request(), "same-key")
        replay = subject.submit(request(), "same-key")
        self.assertTrue(replay.replayed)
        self.assertEqual(replay.delivery_id, first.delivery_id)
        with self.assertRaises(IdempotencyConflict):
            subject.submit(request("O-99"), "same-key")

    def test_transient_failure_is_scheduled_with_injected_time(self) -> None:
        clock = ManualClock(NOW)
        provider = Provider([ProviderResult.transient("503"), ProviderResult.delivered()])
        subject = service(
            provider,
            clock=clock.now,
            retry=RetryPolicy(2, timedelta(seconds=2), timedelta(seconds=10)),
        )
        delivery_id = subject.submit(request(), "retry-key").delivery_id
        self.assertEqual(subject.process_due(), 1)
        self.assertEqual(subject.process_due(), 0)
        self.assertEqual(subject.view(delivery_id).status, DeliveryStatus.RETRY_WAIT)
        clock.advance(timedelta(seconds=2))
        self.assertEqual(subject.process_due(), 1)
        view = subject.view(delivery_id)
        self.assertEqual(view.status, DeliveryStatus.DELIVERED)
        self.assertEqual(len(view.attempts), 2)

    def test_unknown_is_not_resent_until_capability_makes_it_safe(self) -> None:
        provider = Provider([ProviderResult.unknown()], lookup=True)
        subject = service(provider)
        delivery_id = subject.submit(request(), "unknown-key").delivery_id
        subject.process_due()
        self.assertEqual(subject.view(delivery_id).status, DeliveryStatus.UNKNOWN)
        self.assertEqual(subject.process_due(), 0)
        self.assertEqual(len(provider.calls), 1)

        provider.lookup_result = ProviderResult.delivered()
        self.assertTrue(subject.reconcile_unknown(delivery_id))
        self.assertEqual(subject.view(delivery_id).status, DeliveryStatus.DELIVERED)

        unsafe = Provider([ProviderResult.unknown()])
        unsafe_service = service(unsafe)
        unsafe_id = unsafe_service.submit(request(), "unsafe-key").delivery_id
        unsafe_service.process_due()
        self.assertFalse(unsafe_service.reconcile_unknown(unsafe_id))
        self.assertEqual(len(unsafe.calls), 1)

        retrying = Provider([ProviderResult.unknown()] * 2, idempotency=True)
        retry_service = service(retrying)
        retry_id = retry_service.submit(request(), "bounded-key").delivery_id
        retry_service.process_due()
        self.assertTrue(retry_service.reconcile_unknown(retry_id))
        retry_service.process_due()
        self.assertFalse(retry_service.reconcile_unknown(retry_id))
        self.assertEqual(2, len(retrying.calls))

    def test_concurrent_workers_claim_exactly_one_attempt(self) -> None:
        provider = Provider([ProviderResult.delivered()])
        subject = service(provider)
        delivery_id = subject.submit(request(), "race-key").delivery_id
        barrier = Barrier(8)

        def work(_: int) -> int:
            barrier.wait()
            return subject.process_due()

        with ThreadPoolExecutor(max_workers=8) as pool:
            processed = list(pool.map(work, range(8)))
        self.assertEqual(sum(processed), 1)
        self.assertEqual(len(provider.calls), 1)
        self.assertEqual(subject.view(delivery_id).status, DeliveryStatus.DELIVERED)


if __name__ == "__main__":
    unittest.main(verbosity=2)
