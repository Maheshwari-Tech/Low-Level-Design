package com.example.lld.warehouse_fulfilment_domain.demo;

import com.example.lld.warehouse_fulfilment_domain.exception.InvalidScanException;
import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;
import com.example.lld.warehouse_fulfilment_domain.exception.TaskAlreadyClaimedException;
import com.example.lld.warehouse_fulfilment_domain.model.Destination;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentEvent;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentOrder;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentPackage;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentStatus;
import com.example.lld.warehouse_fulfilment_domain.model.OrderLineDemand;
import com.example.lld.warehouse_fulfilment_domain.model.PickTask;
import com.example.lld.warehouse_fulfilment_domain.model.Shipment;
import com.example.lld.warehouse_fulfilment_domain.model.WarehouseAvailability;
import com.example.lld.warehouse_fulfilment_domain.port.CarrierPort;
import com.example.lld.warehouse_fulfilment_domain.port.IdGenerator;
import com.example.lld.warehouse_fulfilment_domain.port.StatusPublisher;
import com.example.lld.warehouse_fulfilment_domain.service.SplitFirstFitAllocator;
import com.example.lld.warehouse_fulfilment_domain.service.WarehouseFulfilmentService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Assertion-backed normal, failure, retry, split, short-pick, and concurrency flows. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T10:15:30Z"), ZoneOffset.UTC);
        AtomicInteger sequence = new AtomicInteger();
        IdGenerator ids = prefix -> prefix + '-' + sequence.incrementAndGet();
        FakeCarrier carrier = new FakeCarrier();
        FakePublisher publisher = new FakePublisher();
        WarehouseFulfilmentService service = new WarehouseFulfilmentService(
                clock, ids, new SplitFirstFitAllocator(), carrier, publisher);
        Destination destination = new Destination(
                "42 Domain Drive", "Bengaluru", "560001", "IN");

        publisher.failNextPublication();
        List<OrderLineDemand> demand = List.of(
                new OrderLineDemand("line-book", "BOOK", 3),
                new OrderLineDemand("line-mug", "MUG", 1));
        List<WarehouseAvailability> availability = List.of(
                new WarehouseAvailability("WH-A", "A-01", "BOOK", 2),
                new WarehouseAvailability("WH-B", "B-01", "BOOK", 1),
                new WarehouseAvailability("WH-B", "B-02", "MUG", 1));
        FulfilmentOrder created = service.createFulfilment(
                "create-100", "order-100", destination, demand, availability);
        FulfilmentOrder duplicateCreation = service.createFulfilment(
                "create-100", "order-100", destination, demand, availability);
        check(created.fulfilmentId().equals(duplicateCreation.fulfilmentId()),
                "idempotent creation returns one fulfilment identity");
        check(created.pickTaskIds().size() == 3,
                "one line is split across warehouses into independent tasks");
        check(service.pendingEventCount() == 1,
                "failed status publication remains in the outbox");
        check(service.publishPendingEvents() == 1 && service.pendingEventCount() == 0,
                "status outbox retries the same immutable event");

        PickTask splitTask = service.tasksForFulfilment(created.fulfilmentId()).stream()
                .filter(task -> task.warehouseId().equals("WH-A"))
                .findFirst()
                .orElseThrow();
        int claimWinners = raceToClaim(service, splitTask.taskId());
        check(claimWinners == 1, "only one worker atomically claims a task");
        String winningWorker = service.getTask(splitTask.taskId()).workerId();
        try {
            service.scanPick(
                    "bad-scan", splitTask.taskId(), winningWorker,
                    "WH-A", "WRONG-BIN", "BOOK", 1);
            throw new AssertionError("wrong-bin scan should fail");
        } catch (InvalidScanException expected) {
            check(service.getTask(splitTask.taskId()).scannedQuantity() == 0,
                    "a rejected scan changes no quantity");
        }
        PickTask partial = service.scanPick(
                "pick-scan-1", splitTask.taskId(), winningWorker,
                "WH-A", "A-01", "BOOK", 1);
        PickTask duplicateScan = service.scanPick(
                "pick-scan-1", splitTask.taskId(), winningWorker,
                "WH-A", "A-01", "BOOK", 1);
        check(partial.scannedQuantity() == 1 && duplicateScan.scannedQuantity() == 1,
                "duplicate physical scan is recorded exactly once");

        FulfilmentOrder reallocated = service.reportShortPick(
                "short-1", splitTask.taskId(), winningWorker, "damaged unit",
                List.of(new WarehouseAvailability("WH-C", "C-01", "BOOK", 1)));
        check(reallocated.pickTaskIds().size() == 4,
                "short quantity creates a replacement task without losing the partial pick");
        check(service.getTask(splitTask.taskId()).status() == PickTask.Status.SHORT_PICKED,
                "original task retains explicit short-pick state");

        for (PickTask task : service.tasksForFulfilment(created.fulfilmentId())) {
            if (task.status() == PickTask.Status.AVAILABLE) {
                completeTask(service, task, "worker-" + task.warehouseId());
            }
        }
        FulfilmentOrder picked = service.getFulfilment(created.fulfilmentId());
        check(picked.status() == FulfilmentStatus.PICKED,
                "full and reallocated tasks resolve the requested quantities exactly");
        check(picked.line("line-book").pickedQuantity() == 3,
                "partial plus replacement picks equal original demand");

        FulfilmentPackage packageA = service.openPackage(
                "open-a", picked.fulfilmentId(), "WH-A");
        FulfilmentPackage packageB = service.openPackage(
                "open-b", picked.fulfilmentId(), "WH-B");
        FulfilmentPackage packageC = service.openPackage(
                "open-c", picked.fulfilmentId(), "WH-C");
        FulfilmentPackage scannedA = service.scanIntoPackage(
                "pack-scan-a", packageA.packageId(), "line-book", "BOOK", 1);
        FulfilmentPackage duplicatePackageScan = service.scanIntoPackage(
                "pack-scan-a", packageA.packageId(), "line-book", "BOOK", 1);
        check(scannedA.contents().get(0).quantity() == 1
                        && duplicatePackageScan.contents().get(0).quantity() == 1,
                "duplicate package scan does not pack an item twice");
        service.scanIntoPackage(
                "pack-scan-b1", packageB.packageId(), "line-book", "BOOK", 1);
        service.scanIntoPackage(
                "pack-scan-b2", packageB.packageId(), "line-mug", "MUG", 1);
        service.scanIntoPackage(
                "pack-scan-c", packageC.packageId(), "line-book", "BOOK", 1);
        service.sealPackage("seal-a", packageA.packageId(), 500);
        service.sealPackage("seal-b", packageB.packageId(), 700);
        service.sealPackage("seal-c", packageC.packageId(), 450);
        check(service.getFulfilment(created.fulfilmentId()).status() == FulfilmentStatus.PACKED,
                "order becomes packed only when all contents are in sealed packages");

        carrier.failNextHandover();
        try {
            service.shipPackage("ship-a", packageA.packageId(), "DHL", "TRACK-A");
            throw new AssertionError("carrier failure should leave package retryable");
        } catch (IllegalStateException expected) {
            check(service.getPackage(packageA.packageId()).status()
                            == FulfilmentPackage.Status.SEALED,
                    "failed hand-off does not create a logical shipment");
        }
        Shipment shipmentA = service.shipPackage(
                "ship-a", packageA.packageId(), "DHL", "TRACK-A");
        Shipment duplicateShipment = service.shipPackage(
                "ship-a", packageA.packageId(), "DHL", "TRACK-A");
        check(shipmentA.shipmentId().equals(duplicateShipment.shipmentId()),
                "duplicate carrier command resolves to one logical shipment");
        service.shipPackage("ship-b", packageB.packageId(), "DHL", "TRACK-B");
        service.shipPackage("ship-c", packageC.packageId(), "DHL", "TRACK-C");
        FulfilmentOrder shipped = service.getFulfilment(created.fulfilmentId());
        check(shipped.status() == FulfilmentStatus.SHIPPED
                        && shipped.shipmentIds().size() == 3,
                "split warehouses produce traceable split shipments");
        check(carrier.successfulHandoverCount() == 3,
                "carrier idempotency prevents duplicate hand-offs");
        try {
            service.cancelFulfilment(
                    "cancel-too-late", shipped.fulfilmentId(), "Customer request");
            throw new AssertionError("shipped fulfilment must not be cancelled");
        } catch (InvalidStateException expected) {
            check(service.getFulfilment(shipped.fulfilmentId()).status()
                            == FulfilmentStatus.SHIPPED,
                    "rejected late cancellation leaves shipment terminal");
        }

        FulfilmentOrder cancellable = service.createFulfilment(
                "create-200",
                "order-200",
                destination,
                List.of(new OrderLineDemand("line-cancel", "PEN", 1)),
                List.of(new WarehouseAvailability("WH-A", "A-02", "PEN", 1)));
        FulfilmentOrder cancelled = service.cancelFulfilment(
                "cancel-200", cancellable.fulfilmentId(), "Cancelled before picking");
        check(cancelled.status() == FulfilmentStatus.CANCELLED
                        && service.getTask(cancelled.pickTaskIds().get(0)).status()
                        == PickTask.Status.CANCELLED,
                "eligible cancellation closes unpicked work");
        try {
            cancelled.auditTrail().add(cancelled.auditTrail().get(0));
            throw new AssertionError("audit trail must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected: callers cannot rewrite execution history.
        }

        System.out.println("Warehouse Fulfilment Domain demo passed");
        System.out.println("  split tasks: " + created.pickTaskIds().size() + " -> "
                + reallocated.pickTaskIds().size());
        System.out.println("  claim winners: " + claimWinners + "/2");
        System.out.println("  shipments: " + shipped.shipmentIds().size());
        System.out.println("  published statuses: " + publisher.publishedCount());
    }

    private static int raceToClaim(
            WarehouseFulfilmentService service, String taskId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<PickTask> first = pool.submit(
                    () -> service.claimTask("claim-a", taskId, "worker-a"));
            Future<PickTask> second = pool.submit(
                    () -> service.claimTask("claim-b", taskId, "worker-b"));
            int winners = 0;
            for (Future<PickTask> result : List.of(first, second)) {
                try {
                    result.get();
                    winners++;
                } catch (ExecutionException expectedLoser) {
                    if (!(expectedLoser.getCause() instanceof TaskAlreadyClaimedException)) {
                        throw expectedLoser;
                    }
                }
            }
            return winners;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void completeTask(
            WarehouseFulfilmentService service, PickTask task, String workerId) {
        PickTask claimed = service.claimTask(
                "claim-" + task.taskId(), task.taskId(), workerId);
        service.scanPick(
                "scan-" + task.taskId(),
                task.taskId(),
                claimed.workerId(),
                task.warehouseId(),
                task.binId(),
                task.sku(),
                task.quantity());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** In-memory idempotent adapter standing in for a carrier API. */
    private static final class FakeCarrier implements CarrierPort {
        private final Map<String, CarrierConfirmation> confirmations = new ConcurrentHashMap<>();
        private final AtomicBoolean failNext = new AtomicBoolean();

        @Override
        public CarrierConfirmation handOver(
                String packageId,
                String carrier,
                String trackingReference,
                String idempotencyKey) {
            if (failNext.compareAndSet(true, false)) {
                throw new IllegalStateException("carrier temporarily unavailable");
            }
            return confirmations.computeIfAbsent(
                    idempotencyKey,
                    ignored -> new CarrierConfirmation(carrier, trackingReference));
        }

        private void failNextHandover() {
            failNext.set(true);
        }

        private int successfulHandoverCount() {
            return confirmations.size();
        }
    }

    /** In-memory upstream adapter with a controllable transient failure. */
    private static final class FakePublisher implements StatusPublisher {
        private final Set<String> published = ConcurrentHashMap.newKeySet();
        private final AtomicBoolean failNext = new AtomicBoolean();

        @Override
        public void publish(FulfilmentEvent event) {
            if (failNext.compareAndSet(true, false)) {
                throw new IllegalStateException("order-status endpoint unavailable");
            }
            published.add(event.eventId());
        }

        private void failNextPublication() {
            failNext.set(true);
        }

        private int publishedCount() {
            return published.size();
        }
    }
}
