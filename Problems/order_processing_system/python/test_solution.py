from collections import deque
from concurrent.futures import ThreadPoolExecutor
from dataclasses import FrozenInstanceError
from datetime import datetime, timezone
from threading import Barrier, Lock
import unittest

from solution import (
    CommandOutcome, IdempotencyConflict, Money, OrderLine, OrderService,
    OrderStatus, RemoteResult, RemoteStatus,
)


class ManualClock:
    def __init__(self):
        self.value = datetime(2026, 1, 1, tzinfo=timezone.utc)

    def now(self):
        return self.value


class InventoryFake:
    def __init__(self, reserves=(), releases=()):
        self.reserves, self.releases = deque(reserves), deque(releases)
        self.reserve_calls, self.release_calls, self.terminal = [], [], {}
        self.lock = Lock()

    def _next(self, calls, scripted, key, default):
        with self.lock:
            calls.append(key)
            if key in self.terminal:
                return self.terminal[key]
            result = scripted.popleft() if scripted else default
            if result.status is not RemoteStatus.UNKNOWN:
                self.terminal[key] = result
            return result

    def reserve(self, order_id, quantities, key):
        return self._next(
            self.reserve_calls, self.reserves, key, RemoteResult.succeeded("R-1")
        )

    def release(self, reservation_id, key):
        return self._next(
            self.release_calls, self.releases, key, RemoteResult.succeeded(reservation_id)
        )


class PaymentFake:
    def __init__(self, results=()):
        self.results, self.calls, self.terminal, self.lock = deque(results), [], {}, Lock()

    def charge(self, order_id, amount, key):
        with self.lock:
            self.calls.append((key, amount))
            if key in self.terminal:
                return self.terminal[key]
            result = self.results.popleft() if self.results else RemoteResult.succeeded("P-1")
            if result.status is not RemoteStatus.UNKNOWN:
                self.terminal[key] = result
            return result


class OrderCoreTest(unittest.TestCase):
    def setUp(self):
        self.clock, self.inventory, self.payments = ManualClock(), InventoryFake(), PaymentFake()
        self.service = OrderService(self.clock, self.inventory, self.payments)
        self.lines = (
            OrderLine("L1", "A", "Tea", 2, Money.parse("500.00", "INR"), Money.parse("50", "INR")),
            OrderLine("L2", "B", "Coffee", 1, Money.parse("300", "INR"), Money.zero("INR")),
        )

    def create(self, service=None, order_id="O1", key="create"):
        return (service or self.service).create_order(key, order_id, "C1", self.lines)

    def test_immutable_price_snapshot_exact_money_and_guarded_transitions(self):
        created = self.create()
        self.assertEqual(Money.parse("1200.00", "INR"), created.order.payable_total)
        with self.assertRaises(FrozenInstanceError):
            created.order.lines[0].unit_price.amount = Money.parse("1", "INR").amount
        self.assertEqual(
            CommandOutcome.REJECTED,
            self.service.fulfil_line("too-soon", "O1", "L1", 1).outcome,
        )
        self.service.confirm_order("confirm", "O1")
        partial = self.service.fulfil_line("fulfil", "O1", "L1", 1)
        self.assertEqual(OrderStatus.PARTIALLY_FULFILLED, partial.order.status)
        self.assertEqual(
            CommandOutcome.REJECTED,
            self.service.fulfil_line("too-many", "O1", "L1", 2).outcome,
        )

    def test_command_replay_is_exact_and_changed_input_conflicts(self):
        created = self.create()
        self.assertIs(created, self.create())
        confirmed = self.service.confirm_order("confirm", "O1")
        self.assertIs(confirmed, self.service.confirm_order("confirm", "O1"))
        self.assertEqual(1, len(self.inventory.reserve_calls))
        self.assertEqual(1, len(self.payments.calls))
        with self.assertRaises(IdempotencyConflict):
            self.service.create_order("create", "O2", "C1", self.lines)

    def test_payment_rejection_compensates_inventory_once(self):
        inventory = InventoryFake(releases=(RemoteResult.succeeded("R9"),))
        payments = PaymentFake((RemoteResult.rejected("declined"),))
        service = OrderService(self.clock, inventory, payments)
        self.create(service, "O9")
        rejected = service.confirm_order("confirm", "O9")
        self.assertIs(rejected, service.confirm_order("confirm", "O9"))
        self.assertEqual(CommandOutcome.REJECTED, rejected.outcome)
        self.assertEqual(1, len(inventory.release_calls))
        self.assertEqual(1, len(payments.calls))

    def test_unknown_payment_is_not_released_and_retry_uses_the_same_key(self):
        inventory = InventoryFake()
        payments = PaymentFake((RemoteResult.unknown("timeout"), RemoteResult.succeeded("P9")))
        service = OrderService(self.clock, inventory, payments)
        self.create(service, "O9")
        self.assertEqual(CommandOutcome.UNKNOWN, service.confirm_order("confirm", "O9").outcome)
        self.assertEqual([], inventory.release_calls)
        self.assertEqual(CommandOutcome.APPLIED, service.confirm_order("confirm", "O9").outcome)
        self.assertEqual(payments.calls[0][0], payments.calls[1][0])

    def test_unknown_compensation_retries_without_recharging(self):
        inventory = InventoryFake(
            releases=(RemoteResult.unknown("timeout"), RemoteResult.succeeded("R1"))
        )
        payments = PaymentFake((RemoteResult.rejected("declined"),))
        service = OrderService(self.clock, inventory, payments)
        self.create(service)
        self.assertEqual(CommandOutcome.UNKNOWN, service.confirm_order("confirm", "O1").outcome)
        self.assertEqual(CommandOutcome.REJECTED, service.confirm_order("confirm", "O1").outcome)
        self.assertEqual(1, len(payments.calls))
        self.assertEqual(2, len(inventory.release_calls))

    def test_concurrent_confirmation_has_one_business_effect(self):
        self.create()
        barrier = Barrier(3)

        def confirm(key):
            barrier.wait()
            return self.service.confirm_order(key, "O1")

        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(confirm, key) for key in ("a", "b")]
            barrier.wait()
            results = [future.result(timeout=2) for future in futures]
        self.assertEqual(1, sum(result.outcome is CommandOutcome.APPLIED for result in results))
        self.assertEqual(1, len(self.inventory.reserve_calls))
        self.assertEqual(1, len(self.payments.calls))


if __name__ == "__main__":
    unittest.main(verbosity=2)
