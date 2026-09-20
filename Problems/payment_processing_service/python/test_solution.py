from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from threading import Barrier, Event, RLock
import unittest

from solution import (
    AmountExceeded,
    AttemptStatus,
    CallbackDisposition,
    IdempotencyConflict,
    Money,
    Operation,
    OperationInProgress,
    PaymentError,
    PaymentService,
    PaymentStatus,
    ProviderCallback,
    ProviderRequest,
    ProviderResponse,
)


TOTAL = Money.of("100.00")


class ScriptedProvider:
    def __init__(self, name: str = "scripted") -> None:
        self.name = name
        self.queues = {operation: [] for operation in Operation}
        self.requests = []
        self.lock = RLock()

    def enqueue(self, operation: Operation, response: ProviderResponse) -> None:
        self.queues[operation].append(response)

    def execute(self, request: ProviderRequest) -> ProviderResponse:
        with self.lock:
            self.requests.append(request)
            if not self.queues[request.operation]:
                raise AssertionError("no scripted response for {}".format(request.operation))
            return self.queues[request.operation].pop(0)


class BlockingProvider(ScriptedProvider):
    def __init__(self) -> None:
        super().__init__("blocking")
        self.entered = Event()
        self.release = Event()

    def execute(self, request: ProviderRequest) -> ProviderResponse:
        self.entered.set()
        if not self.release.wait(timeout=2):
            raise TimeoutError("provider was not released")
        return super().execute(request)


class PaymentServiceTests(unittest.TestCase):
    def service(self, provider: ScriptedProvider) -> PaymentService:
        service = PaymentService([provider])
        service.create("pay-1", "order-1", TOTAL, "tok_test", provider.name, "create-key")
        return service

    def test_partial_capture_and_refund_preserve_exact_totals(self) -> None:
        provider = ScriptedProvider()
        provider.enqueue(Operation.AUTHORIZE, ProviderResponse.approved("auth-1"))
        provider.enqueue(Operation.CAPTURE, ProviderResponse.approved("cap-40"))
        provider.enqueue(Operation.CAPTURE, ProviderResponse.approved("cap-60"))
        provider.enqueue(Operation.REFUND, ProviderResponse.approved("refund-25"))
        service = self.service(provider)

        service.authorize("pay-1", "auth-key")
        partial = service.capture("pay-1", Money.of("40.00"), "cap-40-key")
        self.assertEqual(partial.payment.status, PaymentStatus.PARTIALLY_CAPTURED)
        service.capture("pay-1", Money.of("60.00"), "cap-60-key")
        refunded = service.refund("pay-1", Money.of("25.00"), "refund-key")

        self.assertEqual(refunded.payment.captured, TOTAL)
        self.assertEqual(refunded.payment.refunded, Money.of("25.00"))
        self.assertEqual(refunded.payment.status, PaymentStatus.PARTIALLY_REFUNDED)
        self.assertEqual(len(refunded.payment.attempts), 4)

    def test_concurrent_duplicate_joins_one_provider_call(self) -> None:
        provider = BlockingProvider()
        provider.enqueue(Operation.AUTHORIZE, ProviderResponse.approved("auth-1"))
        service = self.service(provider)
        barrier = Barrier(2)

        def authorize_once():
            barrier.wait()
            return service.authorize("pay-1", "same-key")

        with ThreadPoolExecutor(max_workers=2) as executor:
            first = executor.submit(authorize_once)
            second = executor.submit(authorize_once)
            self.assertTrue(provider.entered.wait(timeout=1))
            provider.release.set()
            first_result = first.result(timeout=2)
            second_result = second.result(timeout=2)

        self.assertEqual(first_result, second_result)
        self.assertEqual(len(provider.requests), 1)

    def test_unknown_blocks_work_until_callback_reconciles(self) -> None:
        provider = ScriptedProvider()
        provider.enqueue(Operation.AUTHORIZE, ProviderResponse.unknown("timeout"))
        service = self.service(provider)
        unknown = service.authorize("pay-1", "auth-key")

        self.assertEqual(unknown.attempt.status, AttemptStatus.UNKNOWN)
        self.assertEqual(unknown.payment.status, PaymentStatus.AUTHORIZING)
        with self.assertRaises(OperationInProgress):
            service.capture("pay-1", Money.of("10.00"), "early-capture")
        self.assertEqual(service.authorize("pay-1", "auth-key"), unknown)
        self.assertEqual(len(provider.requests), 1)

        callback = ProviderCallback(
            " callback-1 ", provider.name, unknown.attempt.request_id, "pay-1",
            Operation.AUTHORIZE, TOTAL, ProviderResponse.approved("late-auth"),
        )
        disposition, payment = service.callback(callback)
        self.assertEqual(disposition, CallbackDisposition.APPLIED)
        self.assertEqual(payment.status, PaymentStatus.AUTHORIZED)
        duplicate, _ = service.callback(callback)
        self.assertEqual(duplicate, CallbackDisposition.DUPLICATE)
        with self.assertRaises(IdempotencyConflict):
            service.callback(ProviderCallback(
                "callback-1", provider.name, unknown.attempt.request_id, "pay-1",
                Operation.AUTHORIZE, TOTAL, ProviderResponse.approved("conflicting-auth"),
            ))
        stale, _ = service.callback(ProviderCallback(
            "callback-2", provider.name, unknown.attempt.request_id, "pay-1",
            Operation.AUTHORIZE, TOTAL, ProviderResponse.approved("late-auth"),
        ))
        self.assertEqual(stale, CallbackDisposition.STALE)

    def test_racing_captures_never_exceed_authorization(self) -> None:
        provider = ScriptedProvider()
        provider.enqueue(Operation.AUTHORIZE, ProviderResponse.approved("auth-1"))
        provider.enqueue(Operation.CAPTURE, ProviderResponse.approved("only-capture"))
        service = self.service(provider)
        service.authorize("pay-1", "auth-key")
        barrier = Barrier(2)

        def capture(amount: str, key: str):
            barrier.wait()
            try:
                return service.capture("pay-1", Money.of(amount), key)
            except PaymentError as error:
                return error

        with ThreadPoolExecutor(max_workers=2) as executor:
            results = list(executor.map(
                lambda args: capture(*args), [("70.00", "cap-70"), ("40.00", "cap-40")]
            ))

        successes = [result for result in results if not isinstance(result, Exception)]
        failures = [result for result in results if isinstance(result, Exception)]
        self.assertEqual((len(successes), len(failures)), (1, 1))
        self.assertIsInstance(failures[0], (AmountExceeded, OperationInProgress))
        self.assertLessEqual(service.get("pay-1").captured.amount, Decimal("100.00"))
        captures = [r for r in provider.requests if r.operation is Operation.CAPTURE]
        self.assertEqual(len(captures), 1)

    def test_money_and_idempotency_reject_ambiguous_reuse(self) -> None:
        with self.assertRaises(TypeError):
            Money(10.1, "USD")
        provider = ScriptedProvider()
        provider.enqueue(Operation.AUTHORIZE, ProviderResponse.approved("auth-1"))
        provider.enqueue(Operation.CAPTURE, ProviderResponse.approved("cap-1"))
        service = self.service(provider)
        service.authorize("pay-1", "auth-key")
        service.capture("pay-1", Money.of("10.00"), "capture-key")
        with self.assertRaises(IdempotencyConflict):
            service.capture("pay-1", Money.of("11.00"), "capture-key")
        created = service.create(" pay-2 ", " order-2 ", TOTAL, " tok-2 ", " scripted ", "create-2")
        self.assertEqual(service.get("pay-2"), created.payment)


if __name__ == "__main__":
    unittest.main(verbosity=2)
