from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from threading import Barrier, Lock
import unittest

from solution import (
    CommandOutcome, IdempotencyConflict, InventoryKey,
    InventoryReservationService, ReservationStatus,
)


class ManualClock:
    def __init__(self):
        self.value = datetime(2026, 1, 1, tzinfo=timezone.utc)

    def now(self):
        return self.value

    def advance(self, delta):
        self.value += delta


class SequenceIds:
    def __init__(self):
        self.value, self.lock = 0, Lock()

    def next_id(self):
        with self.lock:
            self.value += 1
            return "R{}".format(self.value)


class InventoryCoreTest(unittest.TestCase):
    def setUp(self):
        self.clock, self.ids = ManualClock(), SequenceIds()
        self.service = InventoryReservationService(self.clock, self.ids)
        self.a_blr = InventoryKey("A", "BLR")
        self.a_mum = InventoryKey("A", "MUM")
        self.b_mum = InventoryKey("B", "MUM")
        self.service.set_on_hand(self.a_blr, 2)
        self.service.set_on_hand(self.a_mum, 2)
        self.service.set_on_hand(self.b_mum, 1)

    def test_multi_sku_reserve_is_all_or_none_and_first_fit_is_deterministic(self):
        rejected = self.service.reserve(
            "too-large", {"A": 3, "B": 2}, timedelta(minutes=5)
        )
        self.assertEqual(CommandOutcome.REJECTED, rejected.outcome)
        self.assertEqual(0, self.service.get_balance(self.a_blr).reserved)
        self.assertEqual(0, self.service.get_balance(self.b_mum).reserved)

        applied = self.service.reserve(
            "fits", {"B": 1, "A": 3}, timedelta(minutes=5)
        )
        self.assertEqual(CommandOutcome.APPLIED, applied.outcome)
        self.assertEqual(
            [(self.a_blr, 2), (self.a_mum, 1), (self.b_mum, 1)],
            [(item.key, item.quantity) for item in applied.reservation.allocations],
        )

    def test_reserve_replays_exact_result_and_rejects_changed_input(self):
        first = self.service.reserve("same", {"A": 1}, timedelta(seconds=30))
        self.assertIs(first, self.service.reserve("same", {"A": 1}, timedelta(seconds=30)))
        self.assertEqual(1, self.ids.value)
        self.assertEqual(1, self.service.get_balance(self.a_blr).reserved)
        with self.assertRaises(IdempotencyConflict):
            self.service.reserve("same", {"A": 2}, timedelta(seconds=30))

    def test_confirm_consumes_a_hold_once_and_cross_terminal_release_conflicts(self):
        held = self.service.reserve("hold", {"A": 2}, timedelta(minutes=1))
        reservation_id = held.reservation.reservation_id
        before = self.service.get_balance(self.a_blr)
        confirmed = self.service.confirm("confirm", reservation_id)
        self.assertIs(confirmed, self.service.confirm("confirm", reservation_id))
        after = self.service.get_balance(self.a_blr)
        self.assertEqual(before.available, after.available)
        self.assertEqual((0, 0), (after.on_hand, after.reserved))
        self.assertEqual(
            CommandOutcome.APPLIED,
            self.service.confirm("confirm-again", reservation_id).outcome,
        )
        self.assertEqual(
            CommandOutcome.CONFLICT,
            self.service.release("release", reservation_id).outcome,
        )

    def test_release_and_exact_boundary_expiry_restore_holds_only_once(self):
        released = self.service.reserve("release-hold", {"A": 1}, timedelta(minutes=1))
        released_id = released.reservation.reservation_id
        first = self.service.release("release", released_id)
        self.assertIs(first, self.service.release("release", released_id))
        self.assertEqual(0, self.service.get_balance(self.a_blr).reserved)
        self.assertEqual(
            ReservationStatus.RELEASED,
            self.service.release("release-again", released_id).reservation.status,
        )

        expiring = self.service.reserve("expiring", {"B": 1}, timedelta(seconds=5))
        self.clock.advance(timedelta(seconds=5))
        result = self.service.confirm("at-boundary", expiring.reservation.reservation_id)
        self.assertEqual(CommandOutcome.EXPIRED, result.outcome)
        self.assertEqual((1, 0), (
            self.service.get_balance(self.b_mum).on_hand,
            self.service.get_balance(self.b_mum).reserved,
        ))
        self.assertEqual((), self.service.expire_due())

        self.service.reserve("swept", {"A": 1}, timedelta(seconds=5))
        self.clock.advance(timedelta(seconds=5))
        self.assertEqual(1, len(self.service.expire_due()))
        self.assertEqual((), self.service.expire_due())
        self.assertEqual(0, self.service.get_balance(self.a_blr).reserved)

    def test_reversed_multi_key_requests_do_not_deadlock_or_oversell_final_units(self):
        service = InventoryReservationService(self.clock, SequenceIds())
        a, b = InventoryKey("A", "ONE"), InventoryKey("B", "ONE")
        service.set_on_hand(a, 1)
        service.set_on_hand(b, 1)
        barrier = Barrier(3)

        def reserve(key, request):
            barrier.wait()
            return service.reserve(key, request, timedelta(minutes=1))

        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [
                pool.submit(reserve, "left", {"A": 1, "B": 1}),
                pool.submit(reserve, "right", {"B": 1, "A": 1}),
            ]
            barrier.wait()
            results = [future.result(timeout=2) for future in futures]
        self.assertEqual(1, sum(item.outcome is CommandOutcome.APPLIED for item in results))
        self.assertEqual(1, sum(item.outcome is CommandOutcome.REJECTED for item in results))
        self.assertEqual((1, 1), (
            service.get_balance(a).reserved, service.get_balance(b).reserved
        ))


if __name__ == "__main__":
    unittest.main(verbosity=2)
