from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
import unittest

from solution import (
    ClaimConflict,
    FulfilmentStatus,
    IdempotencyConflict,
    InvalidState,
    OrderLineDemand,
    PackageStatus,
    QuantityError,
    SplitFirstFitAllocator,
    TaskStatus,
    WarehouseAvailability,
    WarehouseFulfilmentService,
)


class WarehouseFulfilmentTests(unittest.TestCase):
    def setUp(self) -> None:
        self.service = WarehouseFulfilmentService(SplitFirstFitAllocator())

    @staticmethod
    def availability(quantity: int = 3):  # type: ignore[no-untyped-def]
        return [WarehouseAvailability("WH-1", "BIN-A", "SKU-A", quantity)]

    def create(self, quantity: int = 3):  # type: ignore[no-untyped-def]
        return self.service.create_order(
            "order-1",
            [OrderLineDemand("line-a", "SKU-A", quantity)],
            self.availability(quantity),
        )

    def pick_all(self, quantity: int = 3):  # type: ignore[no-untyped-def]
        task = next(iter(self.create(quantity).tasks.values()))
        claimed = self.service.claim_task(task.task_id, "worker-1")
        assert claimed.claim_token is not None
        return self.service.scan(
            "scan-1", task.task_id, "worker-1", claimed.claim_token, quantity
        )

    def test_split_allocation_is_complete_and_deterministic(self) -> None:
        order = self.service.create_order(
            "order-1",
            [OrderLineDemand("line-a", "SKU-A", 7)],
            [
                WarehouseAvailability("WH-2", "B-9", "SKU-A", 2),
                WarehouseAvailability("WH-1", "A-1", "SKU-A", 5),
            ],
        )

        self.assertEqual(
            [
                (task.warehouse_id, task.bin_id, task.quantity)
                for task in order.tasks.values()
            ],
            [("WH-1", "A-1", 5), ("WH-2", "B-9", 2)],
        )
        self.assertEqual(
            order.lines["line-a"].allocated, order.lines["line-a"].requested
        )

    def test_exactly_one_worker_wins_a_concurrent_claim(self) -> None:
        task = next(iter(self.create().tasks.values()))
        barrier = Barrier(12)

        def compete(index: int):  # type: ignore[no-untyped-def]
            barrier.wait()
            try:
                return self.service.claim_task(task.task_id, "worker-{}".format(index))
            except ClaimConflict as error:
                return error

        with ThreadPoolExecutor(max_workers=12) as executor:
            results = list(executor.map(compete, range(12)))

        winners = [result for result in results if not isinstance(result, Exception)]
        self.assertEqual(len(winners), 1)
        self.assertEqual(winners[0].status, TaskStatus.CLAIMED)

    def test_scan_is_idempotent_and_quantity_guarded(self) -> None:
        task = next(iter(self.create(quantity=3).tasks.values()))
        claimed = self.service.claim_task(task.task_id, "worker-1")
        assert claimed.claim_token is not None

        first = self.service.scan(
            " scan-1 ", task.task_id, "worker-1", claimed.claim_token, 2
        )
        replay = self.service.scan(
            "scan-1", task.task_id, "worker-1", claimed.claim_token, 2
        )
        self.assertEqual((first.picked, replay.picked), (2, 2))
        with self.assertRaises(IdempotencyConflict):
            self.service.scan(
                "scan-1", task.task_id, "worker-1", claimed.claim_token, 1
            )
        with self.assertRaises(QuantityError):
            self.service.scan(
                "scan-2", task.task_id, "worker-1", claimed.claim_token, 2
            )

    def test_pack_and_ship_commands_are_idempotent_and_conserve_quantity(self) -> None:
        self.pick_all(quantity=3)
        self.service.create_package("package-1", "order-1", "WH-1")
        self.service.create_package("package-2", "order-1", "WH-1")

        first = self.service.pack("pack-1", "package-1", "line-a", 3)
        replay = self.service.pack("pack-1", "package-1", "line-a", 3)
        self.assertEqual(first, replay)
        with self.assertRaises(QuantityError):
            self.service.pack("pack-2", "package-2", "line-a", 1)
        with self.assertRaises(InvalidState):
            self.service.ship_package("ship-early", "package-1", "TRACK-1")

        self.service.seal_package("seal-1", "package-1")
        shipped = self.service.ship_package("ship-1", "package-1", "TRACK-1")
        duplicate = self.service.ship_package("ship-1", "package-1", "TRACK-1")
        self.assertEqual(shipped, duplicate)
        self.assertEqual(shipped.status, PackageStatus.SHIPPED)
        final = self.service.get_order("order-1")
        self.assertEqual(final.status, FulfilmentStatus.SHIPPED)
        self.assertEqual(
            (
                final.lines["line-a"].picked,
                final.lines["line-a"].packed,
                final.lines["line-a"].shipped,
            ),
            (3, 3, 3),
        )

    def test_short_pick_records_truth_then_recovery_creates_new_work(self) -> None:
        task = next(iter(self.create(quantity=5).tasks.values()))
        claimed = self.service.claim_task(task.task_id, "worker-1")
        assert claimed.claim_token is not None
        self.service.scan(
            "scan-1", task.task_id, "worker-1", claimed.claim_token, 3
        )

        short = self.service.short_pick(
            "short-1", task.task_id, "worker-1", claimed.claim_token, "bin empty"
        )
        self.assertEqual(short.status, FulfilmentStatus.EXCEPTION)
        original = short.tasks[task.task_id]
        self.assertEqual((original.picked, original.short), (3, 2))
        self.assertEqual(
            (short.lines["line-a"].allocated, short.lines["line-a"].picked), (3, 3)
        )

        recovered = self.service.recover_shortage(
            "recover-1",
            "order-1",
            "line-a",
            [WarehouseAvailability("WH-2", "BIN-Z", "SKU-A", 2)],
        )
        replay = self.service.recover_shortage(
            "recover-1",
            "order-1",
            "line-a",
            [WarehouseAvailability("WH-2", "BIN-Z", "SKU-A", 2)],
        )
        self.assertEqual(recovered, replay)
        self.assertEqual(recovered.status, FulfilmentStatus.PICKING)
        self.assertEqual(recovered.lines["line-a"].allocated, 5)
        replacement = list(recovered.tasks.values())[-1]
        self.assertEqual(
            (replacement.warehouse_id, replacement.quantity),
            ("WH-2", 2),
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
