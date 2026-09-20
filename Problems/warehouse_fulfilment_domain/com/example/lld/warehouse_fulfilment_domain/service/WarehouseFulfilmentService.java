package com.example.lld.warehouse_fulfilment_domain.service;

import com.example.lld.warehouse_fulfilment_domain.exception.AllocationException;
import com.example.lld.warehouse_fulfilment_domain.exception.IdempotencyConflictException;
import com.example.lld.warehouse_fulfilment_domain.exception.InvalidScanException;
import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;
import com.example.lld.warehouse_fulfilment_domain.exception.NotFoundException;
import com.example.lld.warehouse_fulfilment_domain.model.Allocation;
import com.example.lld.warehouse_fulfilment_domain.model.AuditEntry;
import com.example.lld.warehouse_fulfilment_domain.model.Destination;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentEvent;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentLine;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentOrder;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentPackage;
import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentStatus;
import com.example.lld.warehouse_fulfilment_domain.model.OrderLineDemand;
import com.example.lld.warehouse_fulfilment_domain.model.PackageContent;
import com.example.lld.warehouse_fulfilment_domain.model.PickTask;
import com.example.lld.warehouse_fulfilment_domain.model.Shipment;
import com.example.lld.warehouse_fulfilment_domain.model.WarehouseAvailability;
import com.example.lld.warehouse_fulfilment_domain.port.AllocationStrategy;
import com.example.lld.warehouse_fulfilment_domain.port.CarrierPort;
import com.example.lld.warehouse_fulfilment_domain.port.IdGenerator;
import com.example.lld.warehouse_fulfilment_domain.port.StatusPublisher;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe in-memory application service. The fair lock is the transaction boundary
 * for task claims, scans, counters, packages, shipments, idempotency, and the event outbox.
 */
public final class WarehouseFulfilmentService {
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final AllocationStrategy allocator;
    private final CarrierPort carrier;
    private final StatusPublisher statusPublisher;
    private final ReentrantLock transactionLock = new ReentrantLock(true);
    private final Map<String, FulfilmentOrder> orders = new HashMap<>();
    private final Map<String, String> fulfilmentByExternalOrder = new HashMap<>();
    private final Map<String, String> creationFingerprintByExternalOrder = new HashMap<>();
    private final Map<String, PickTask> tasks = new HashMap<>();
    private final Map<String, FulfilmentPackage> packages = new HashMap<>();
    private final Map<String, Shipment> shipments = new HashMap<>();
    private final Map<String, CommandRecord> completedCommands = new HashMap<>();
    private final Map<String, FulfilmentEvent> pendingEvents = new LinkedHashMap<>();
    private final Set<String> publishedEventIds = new HashSet<>();

    public WarehouseFulfilmentService(
            Clock clock,
            IdGenerator idGenerator,
            AllocationStrategy allocator,
            CarrierPort carrier,
            StatusPublisher statusPublisher) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.carrier = Objects.requireNonNull(carrier, "carrier");
        this.statusPublisher = Objects.requireNonNull(statusPublisher, "statusPublisher");
    }

    public FulfilmentOrder createFulfilment(
            String idempotencyKey,
            String externalOrderId,
            Destination destination,
            List<OrderLineDemand> demand,
            List<WarehouseAvailability> availability) {
        requireKey(idempotencyKey);
        requireText(externalOrderId, "externalOrderId");
        Objects.requireNonNull(destination, "destination");
        List<OrderLineDemand> demandSnapshot = validatedDemand(demand);
        List<WarehouseAvailability> availabilitySnapshot = List.copyOf(availability);
        String fingerprint = creationFingerprint(
                externalOrderId, destination, demandSnapshot, availabilitySnapshot);
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("create", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requireOrder(duplicateId);
            }
            String existingId = fulfilmentByExternalOrder.get(externalOrderId);
            if (existingId != null) {
                if (!creationFingerprintByExternalOrder.get(externalOrderId).equals(fingerprint)) {
                    throw new IdempotencyConflictException("external-order:" + externalOrderId);
                }
                remember("create", idempotencyKey, fingerprint, existingId);
                return requireOrder(existingId);
            }

            List<Allocation> allocations = allocator.allocate(
                    demandSnapshot, availabilitySnapshot);
            validateCompleteAllocation(demandSnapshot, availabilitySnapshot, allocations);
            String fulfilmentId = idGenerator.nextId("fulfilment");
            List<FulfilmentLine> lines = demandSnapshot.stream()
                    .map(FulfilmentLine::from)
                    .toList();
            Map<String, FulfilmentLine> lineById = lineMap(lines);
            List<String> taskIds = new ArrayList<>();
            List<PickTask> newTasks = new ArrayList<>();
            for (Allocation allocation : allocations) {
                lineById.put(
                        allocation.orderLineId(),
                        lineById.get(allocation.orderLineId()).allocate(allocation.quantity()));
                String taskId = idGenerator.nextId("pick");
                taskIds.add(taskId);
                newTasks.add(PickTask.available(
                        taskId, fulfilmentId, allocation, destination));
            }

            FulfilmentOrder received = FulfilmentOrder.received(
                    fulfilmentId,
                    externalOrderId,
                    destination,
                    List.copyOf(lineById.values()),
                    clock.instant(),
                    audit("FULFILMENT_RECEIVED", "order-service",
                            "Confirmed order demand accepted"));
            FulfilmentOrder allocated = received.addTasks(
                    taskIds,
                    received.lines(),
                    FulfilmentStatus.ALLOCATED,
                    audit("DEMAND_ALLOCATED", "allocator",
                            allocations.size() + " pick task(s) across "
                                    + distinctWarehouseCount(allocations) + " warehouse(s)"));
            for (PickTask task : newTasks) {
                tasks.put(task.taskId(), task);
            }
            fulfilmentByExternalOrder.put(externalOrderId, fulfilmentId);
            creationFingerprintByExternalOrder.put(externalOrderId, fingerprint);
            saveOrder(null, allocated, "Order demand allocated");
            remember("create", idempotencyKey, fingerprint, fulfilmentId);
            return allocated;
        } finally {
            transactionLock.unlock();
        }
    }

    public PickTask claimTask(String idempotencyKey, String taskId, String workerId) {
        requireKey(idempotencyKey);
        String fingerprint = taskId + '|' + workerId;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("claim", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requireTask(duplicateId);
            }
            PickTask current = requireTask(taskId);
            PickTask claimed = current.claim(workerId);
            remember("claim", idempotencyKey, fingerprint, taskId);
            if (claimed == current) {
                return current;
            }
            tasks.put(taskId, claimed);
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            FulfilmentOrder updated = order.transition(
                    FulfilmentStatus.PICKING,
                    audit("PICK_TASK_CLAIMED", workerId, "task=" + taskId));
            saveOrder(order, updated, "Picking started");
            return claimed;
        } finally {
            transactionLock.unlock();
        }
    }

    public PickTask scanPick(
            String scanId,
            String taskId,
            String workerId,
            String warehouseId,
            String binId,
            String sku,
            int quantity) {
        requireKey(scanId);
        String fingerprint = taskId + '|' + workerId + '|' + warehouseId + '|'
                + binId + '|' + sku + '|' + quantity;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("pick-scan", scanId, fingerprint);
            if (duplicateId != null) {
                return requireTask(duplicateId);
            }
            PickTask current = requireTask(taskId);
            PickTask scanned = current.scan(
                    workerId, warehouseId, binId, sku, quantity);
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            FulfilmentLine line = order.line(current.orderLineId()).recordPick(quantity);
            tasks.put(taskId, scanned);
            FulfilmentStatus nextStatus = derivePickingStatus(order, line);
            FulfilmentOrder updated = order.updateLine(
                    line,
                    nextStatus,
                    audit("PICK_SCAN_ACCEPTED", workerId,
                            "scan=" + scanId + ", task=" + taskId + ", quantity=" + quantity));
            saveOrder(order, updated,
                    nextStatus == FulfilmentStatus.PICKED
                            ? "All allocated units picked" : "Pick progress recorded");
            remember("pick-scan", scanId, fingerprint, taskId);
            return scanned;
        } finally {
            transactionLock.unlock();
        }
    }

    /**
     * Closes a partial task, removes the short allocation, then creates replacement
     * tasks. If replacement is unavailable, the order enters EXCEPTION without losing
     * the picked quantity or short-pick reason.
     */
    public FulfilmentOrder reportShortPick(
            String idempotencyKey,
            String taskId,
            String workerId,
            String reason,
            List<WarehouseAvailability> replacementAvailability) {
        requireKey(idempotencyKey);
        requireText(reason, "reason");
        List<WarehouseAvailability> replacement = List.copyOf(replacementAvailability);
        String fingerprint = taskId + '|' + workerId + '|' + reason + '|'
                + canonicalAvailability(replacement);
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("short-pick", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requireOrder(duplicateId);
            }
            PickTask current = requireTask(taskId);
            PickTask shortTask = current.closeShort(workerId);
            int shortQuantity = shortTask.remainingQuantity();
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            FulfilmentLine shortenedLine = order.line(current.orderLineId())
                    .removeShortAllocation(shortQuantity);
            tasks.put(taskId, shortTask);
            FulfilmentOrder updated;
            try {
                OrderLineDemand remainingDemand = new OrderLineDemand(
                        current.orderLineId(), current.sku(), shortQuantity);
                List<Allocation> reallocations = allocator.allocate(
                        List.of(remainingDemand), replacement);
                validateCompleteAllocation(
                        List.of(remainingDemand), replacement, reallocations);
                FulfilmentLine reallocatedLine = shortenedLine;
                List<String> replacementTaskIds = new ArrayList<>();
                for (Allocation allocation : reallocations) {
                    reallocatedLine = reallocatedLine.allocate(allocation.quantity());
                    String replacementTaskId = idGenerator.nextId("pick");
                    replacementTaskIds.add(replacementTaskId);
                    tasks.put(
                            replacementTaskId,
                            PickTask.available(
                                    replacementTaskId, order.fulfilmentId(), allocation,
                                    order.destination()));
                }
                List<FulfilmentLine> lines = replaceLine(order.lines(), reallocatedLine);
                FulfilmentStatus nextStatus = hasClaimedTask(order)
                        ? FulfilmentStatus.PICKING : FulfilmentStatus.ALLOCATED;
                updated = order.addTasks(
                        replacementTaskIds,
                        lines,
                        nextStatus,
                        audit("SHORT_PICK_REALLOCATED", workerId,
                                "task=" + taskId + ", short=" + shortQuantity
                                        + ", reason=" + reason));
            } catch (AllocationException unavailable) {
                updated = order.updateLine(
                        shortenedLine,
                        FulfilmentStatus.EXCEPTION,
                        audit("SHORT_PICK_EXCEPTION", workerId,
                                "task=" + taskId + ", short=" + shortQuantity
                                        + ", reason=" + reason + "; " + unavailable.getMessage()));
            }
            saveOrder(order, updated,
                    updated.status() == FulfilmentStatus.EXCEPTION
                            ? "Short pick requires intervention" : "Short quantity reallocated");
            remember("short-pick", idempotencyKey, fingerprint, order.fulfilmentId());
            return updated;
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentPackage openPackage(
            String idempotencyKey, String fulfilmentId, String warehouseId) {
        requireKey(idempotencyKey);
        String fingerprint = fulfilmentId + '|' + warehouseId;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("open-package", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requirePackage(duplicateId);
            }
            FulfilmentOrder order = requireOrder(fulfilmentId);
            if (order.status() != FulfilmentStatus.PICKED) {
                throw new InvalidStateException(
                        "Packages can be opened only after all pick work is resolved");
            }
            requireText(warehouseId, "warehouseId");
            boolean hasPickedWork = tasksFor(order).stream()
                    .anyMatch(task -> task.warehouseId().equals(warehouseId)
                            && task.scannedQuantity() > 0);
            if (!hasPickedWork) {
                throw new InvalidStateException(
                        "Warehouse " + warehouseId + " has no picked contents for this order");
            }
            String packageId = idGenerator.nextId("package");
            FulfilmentPackage opened = FulfilmentPackage.open(
                    packageId, fulfilmentId, warehouseId);
            packages.put(packageId, opened);
            FulfilmentOrder updated = order.addPackage(
                    packageId,
                    audit("PACKAGE_OPENED", "packer", "package=" + packageId
                            + ", warehouse=" + warehouseId));
            saveOrder(order, updated, "Package opened");
            remember("open-package", idempotencyKey, fingerprint, packageId);
            return opened;
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentPackage scanIntoPackage(
            String scanId,
            String packageId,
            String orderLineId,
            String sku,
            int quantity) {
        requireKey(scanId);
        String fingerprint = packageId + '|' + orderLineId + '|' + sku + '|' + quantity;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("package-scan", scanId, fingerprint);
            if (duplicateId != null) {
                return requirePackage(duplicateId);
            }
            FulfilmentPackage current = requirePackage(packageId);
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            FulfilmentLine line = order.line(orderLineId);
            if (!line.sku().equals(sku)) {
                throw new InvalidScanException("Scanned SKU does not match order line");
            }
            int pickedAtWarehouse = tasksFor(order).stream()
                    .filter(task -> task.orderLineId().equals(orderLineId))
                    .filter(task -> task.warehouseId().equals(current.warehouseId()))
                    .mapToInt(PickTask::scannedQuantity)
                    .sum();
            int alreadyPackedAtWarehouse = packagesFor(order).stream()
                    .filter(itemPackage -> itemPackage.warehouseId().equals(current.warehouseId()))
                    .flatMap(itemPackage -> itemPackage.contents().stream())
                    .filter(content -> content.orderLineId().equals(orderLineId))
                    .mapToInt(PackageContent::quantity)
                    .sum();
            if (quantity <= 0 || alreadyPackedAtWarehouse + quantity > pickedAtWarehouse) {
                throw new InvalidScanException(
                        "Package scan exceeds picked/unpacked quantity at warehouse "
                                + current.warehouseId());
            }
            FulfilmentPackage packed = current.addContent(orderLineId, sku, quantity);
            FulfilmentLine updatedLine = line.recordPack(quantity);
            packages.put(packageId, packed);
            FulfilmentOrder updated = order.updateLine(
                    updatedLine,
                    order.status(),
                    audit("PACKAGE_SCAN_ACCEPTED", "packer",
                            "scan=" + scanId + ", package=" + packageId
                                    + ", line=" + orderLineId + ", quantity=" + quantity));
            saveOrder(order, updated, "Package content scanned");
            remember("package-scan", scanId, fingerprint, packageId);
            return packed;
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentPackage sealPackage(
            String idempotencyKey, String packageId, int weightGrams) {
        requireKey(idempotencyKey);
        String fingerprint = packageId + "|weight=" + weightGrams;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("seal", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requirePackage(duplicateId);
            }
            FulfilmentPackage current = requirePackage(packageId);
            FulfilmentPackage sealed = current.seal(weightGrams);
            packages.put(packageId, sealed);
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            boolean allPacked = order.lines().stream()
                    .allMatch(line -> line.packedQuantity() + line.cancelledQuantity()
                            == line.requestedQuantity());
            boolean allPackagesSealed = packagesFor(order).stream()
                    .allMatch(itemPackage -> itemPackage.status()
                            != FulfilmentPackage.Status.OPEN);
            FulfilmentStatus nextStatus = allPacked && allPackagesSealed
                    ? FulfilmentStatus.PACKED : FulfilmentStatus.PICKED;
            FulfilmentOrder updated = order.transition(
                    nextStatus,
                    audit("PACKAGE_SEALED", "packer",
                            "package=" + packageId + ", weightGrams=" + weightGrams));
            saveOrder(order, updated,
                    nextStatus == FulfilmentStatus.PACKED
                            ? "All picked contents packed" : "Package sealed");
            remember("seal", idempotencyKey, fingerprint, packageId);
            return sealed;
        } finally {
            transactionLock.unlock();
        }
    }

    public Shipment shipPackage(
            String idempotencyKey,
            String packageId,
            String requestedCarrier,
            String requestedTrackingReference) {
        requireKey(idempotencyKey);
        String fingerprint = packageId + '|' + requestedCarrier + '|'
                + requestedTrackingReference;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("ship", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requireShipment(duplicateId);
            }
            FulfilmentPackage current = requirePackage(packageId);
            if (current.status() == FulfilmentPackage.Status.SHIPPED) {
                remember("ship", idempotencyKey, fingerprint, current.shipmentId());
                return requireShipment(current.shipmentId());
            }
            if (current.status() != FulfilmentPackage.Status.SEALED) {
                throw new InvalidStateException(
                        "Package " + packageId + " cannot ship from " + current.status());
            }
            CarrierPort.CarrierConfirmation confirmation = carrier.handOver(
                    packageId,
                    requireText(requestedCarrier, "requestedCarrier"),
                    requireText(requestedTrackingReference, "requestedTrackingReference"),
                    "carrier:" + idempotencyKey);
            String shipmentId = idGenerator.nextId("shipment");
            Shipment shipment = new Shipment(
                    shipmentId,
                    current.fulfilmentId(),
                    packageId,
                    confirmation.carrier(),
                    confirmation.trackingNumber(),
                    Shipment.State.SHIPPED,
                    clock.instant());
            FulfilmentOrder order = requireOrder(current.fulfilmentId());
            Map<String, FulfilmentLine> lines = lineMap(order.lines());
            for (PackageContent content : current.contents()) {
                lines.put(
                        content.orderLineId(),
                        lines.get(content.orderLineId()).recordShipment(content.quantity()));
            }
            shipments.put(shipmentId, shipment);
            packages.put(packageId, current.markShipped(shipmentId));
            boolean allShipped = lines.values().stream()
                    .allMatch(line -> line.shippedQuantity() + line.cancelledQuantity()
                            == line.requestedQuantity());
            FulfilmentStatus nextStatus = allShipped
                    ? FulfilmentStatus.SHIPPED : FulfilmentStatus.PARTIALLY_SHIPPED;
            FulfilmentOrder updated = order.addShipment(
                    shipmentId,
                    List.copyOf(lines.values()),
                    nextStatus,
                    audit("PACKAGE_SHIPPED", "carrier",
                            "package=" + packageId + ", carrier=" + confirmation.carrier()
                                    + ", tracking=" + confirmation.trackingNumber()));
            saveOrder(order, updated,
                    allShipped ? "All packages shipped" : "Package shipped");
            remember("ship", idempotencyKey, fingerprint, shipmentId);
            return shipment;
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentOrder cancelFulfilment(
            String idempotencyKey, String fulfilmentId, String reason) {
        requireKey(idempotencyKey);
        requireText(reason, "reason");
        String fingerprint = fulfilmentId + '|' + reason;
        transactionLock.lock();
        try {
            String duplicateId = duplicateReference("cancel", idempotencyKey, fingerprint);
            if (duplicateId != null) {
                return requireOrder(duplicateId);
            }
            FulfilmentOrder order = requireOrder(fulfilmentId);
            if (order.status() == FulfilmentStatus.CANCELLED) {
                remember("cancel", idempotencyKey, fingerprint, fulfilmentId);
                return order;
            }
            if (order.lines().stream().anyMatch(line -> line.pickedQuantity() > 0)) {
                throw new InvalidStateException(
                        "Fulfilment cannot be cancelled after physical picking has started");
            }
            for (PickTask task : tasksFor(order)) {
                tasks.put(task.taskId(), task.cancel());
            }
            for (FulfilmentPackage itemPackage : packagesFor(order)) {
                packages.put(itemPackage.packageId(), itemPackage.cancelEmpty());
            }
            List<FulfilmentLine> cancelledLines = order.lines().stream()
                    .map(FulfilmentLine::cancelUnpicked)
                    .toList();
            FulfilmentOrder cancelled = order.updateLines(
                    cancelledLines,
                    FulfilmentStatus.CANCELLED,
                    audit("FULFILMENT_CANCELLED", "order-service", reason));
            saveOrder(order, cancelled, reason);
            remember("cancel", idempotencyKey, fingerprint, fulfilmentId);
            return cancelled;
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentOrder getFulfilment(String fulfilmentId) {
        transactionLock.lock();
        try {
            return requireOrder(fulfilmentId);
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentOrder getByExternalOrder(String externalOrderId) {
        transactionLock.lock();
        try {
            String fulfilmentId = fulfilmentByExternalOrder.get(externalOrderId);
            if (fulfilmentId == null) {
                throw new NotFoundException("External order fulfilment", externalOrderId);
            }
            return requireOrder(fulfilmentId);
        } finally {
            transactionLock.unlock();
        }
    }

    public PickTask getTask(String taskId) {
        transactionLock.lock();
        try {
            return requireTask(taskId);
        } finally {
            transactionLock.unlock();
        }
    }

    public FulfilmentPackage getPackage(String packageId) {
        transactionLock.lock();
        try {
            return requirePackage(packageId);
        } finally {
            transactionLock.unlock();
        }
    }

    public Shipment getShipment(String shipmentId) {
        transactionLock.lock();
        try {
            return requireShipment(shipmentId);
        } finally {
            transactionLock.unlock();
        }
    }

    public List<PickTask> tasksForFulfilment(String fulfilmentId) {
        transactionLock.lock();
        try {
            return tasksFor(requireOrder(fulfilmentId));
        } finally {
            transactionLock.unlock();
        }
    }

    public List<FulfilmentPackage> packagesForFulfilment(String fulfilmentId) {
        transactionLock.lock();
        try {
            return packagesFor(requireOrder(fulfilmentId));
        } finally {
            transactionLock.unlock();
        }
    }

    public List<Shipment> shipmentsForFulfilment(String fulfilmentId) {
        transactionLock.lock();
        try {
            FulfilmentOrder order = requireOrder(fulfilmentId);
            return order.shipmentIds().stream().map(this::requireShipment).toList();
        } finally {
            transactionLock.unlock();
        }
    }

    /** Retries the immutable event outbox; successfully published event IDs are never sent again. */
    public int publishPendingEvents() {
        transactionLock.lock();
        try {
            return publishPendingEventsLocked();
        } finally {
            transactionLock.unlock();
        }
    }

    public int pendingEventCount() {
        transactionLock.lock();
        try {
            return pendingEvents.size();
        } finally {
            transactionLock.unlock();
        }
    }

    private void saveOrder(
            FulfilmentOrder previous, FulfilmentOrder updated, String details) {
        orders.put(updated.fulfilmentId(), updated);
        if (previous == null || previous.status() != updated.status()) {
            FulfilmentEvent event = new FulfilmentEvent(
                    idGenerator.nextId("event"),
                    updated.fulfilmentId(),
                    updated.externalOrderId(),
                    updated.status(),
                    clock.instant(),
                    details);
            pendingEvents.put(event.eventId(), event);
            publishPendingEventsLocked();
        }
    }

    private int publishPendingEventsLocked() {
        int published = 0;
        Iterator<Map.Entry<String, FulfilmentEvent>> iterator = pendingEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FulfilmentEvent> entry = iterator.next();
            if (publishedEventIds.contains(entry.getKey())) {
                iterator.remove();
                continue;
            }
            try {
                statusPublisher.publish(entry.getValue());
                publishedEventIds.add(entry.getKey());
                iterator.remove();
                published++;
            } catch (RuntimeException retryLater) {
                // Preserve the event and continue; a later command or explicit retry can publish it.
            }
        }
        return published;
    }

    private FulfilmentStatus derivePickingStatus(
            FulfilmentOrder order, FulfilmentLine changedLine) {
        boolean tasksResolved = tasksFor(order).stream().allMatch(task ->
                task.status() == PickTask.Status.COMPLETED
                        || task.status() == PickTask.Status.SHORT_PICKED
                        || task.status() == PickTask.Status.CANCELLED);
        List<FulfilmentLine> lines = replaceLine(order.lines(), changedLine);
        boolean demandResolved = lines.stream().allMatch(line ->
                line.pickedQuantity() + line.cancelledQuantity() == line.requestedQuantity());
        return tasksResolved && demandResolved
                ? FulfilmentStatus.PICKED : FulfilmentStatus.PICKING;
    }

    private boolean hasClaimedTask(FulfilmentOrder order) {
        return tasksFor(order).stream()
                .anyMatch(task -> task.status() == PickTask.Status.CLAIMED);
    }

    private List<PickTask> tasksFor(FulfilmentOrder order) {
        return order.pickTaskIds().stream().map(this::requireTask).toList();
    }

    private List<FulfilmentPackage> packagesFor(FulfilmentOrder order) {
        return order.packageIds().stream().map(this::requirePackage).toList();
    }

    private FulfilmentOrder requireOrder(String fulfilmentId) {
        FulfilmentOrder order = orders.get(fulfilmentId);
        if (order == null) {
            throw new NotFoundException("Fulfilment", fulfilmentId);
        }
        return order;
    }

    private PickTask requireTask(String taskId) {
        PickTask task = tasks.get(taskId);
        if (task == null) {
            throw new NotFoundException("Pick task", taskId);
        }
        return task;
    }

    private FulfilmentPackage requirePackage(String packageId) {
        FulfilmentPackage itemPackage = packages.get(packageId);
        if (itemPackage == null) {
            throw new NotFoundException("Package", packageId);
        }
        return itemPackage;
    }

    private Shipment requireShipment(String shipmentId) {
        Shipment shipment = shipments.get(shipmentId);
        if (shipment == null) {
            throw new NotFoundException("Shipment", shipmentId);
        }
        return shipment;
    }

    private AuditEntry audit(String action, String actor, String details) {
        return new AuditEntry(
                idGenerator.nextId("audit"), action, actor, clock.instant(), details);
    }

    private String duplicateReference(String operation, String key, String fingerprint) {
        CommandRecord record = completedCommands.get(operation + ':' + key);
        if (record == null) {
            return null;
        }
        if (!record.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        return record.resultId();
    }

    private void remember(
            String operation, String key, String fingerprint, String resultId) {
        completedCommands.put(
                operation + ':' + key, new CommandRecord(fingerprint, resultId));
    }

    private static List<OrderLineDemand> validatedDemand(List<OrderLineDemand> demand) {
        List<OrderLineDemand> result = List.copyOf(demand);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("at least one order line is required");
        }
        Set<String> lineIds = new HashSet<>();
        if (result.stream().anyMatch(line -> !lineIds.add(line.orderLineId()))) {
            throw new IllegalArgumentException("orderLineId values must be unique");
        }
        return result;
    }

    private static void validateCompleteAllocation(
            List<OrderLineDemand> demand,
            List<WarehouseAvailability> availability,
            List<Allocation> allocations) {
        Map<String, OrderLineDemand> demandByLine = new HashMap<>();
        Map<String, Integer> allocatedByLine = new HashMap<>();
        for (OrderLineDemand line : demand) {
            demandByLine.put(line.orderLineId(), line);
        }
        Map<String, Integer> capacity = new HashMap<>();
        for (WarehouseAvailability item : availability) {
            capacity.merge(availabilityKey(
                    item.warehouseId(), item.binId(), item.sku()),
                    item.availableQuantity(), Math::addExact);
        }
        Map<String, Integer> usedCapacity = new HashMap<>();
        for (Allocation allocation : allocations) {
            OrderLineDemand line = demandByLine.get(allocation.orderLineId());
            if (line == null || !line.sku().equals(allocation.sku())) {
                throw new AllocationException(
                        "Allocator returned an unknown line or mismatched SKU");
            }
            allocatedByLine.merge(
                    allocation.orderLineId(), allocation.quantity(), Math::addExact);
            String capacityKey = availabilityKey(
                    allocation.warehouseId(), allocation.binId(), allocation.sku());
            int used = usedCapacity.merge(capacityKey, allocation.quantity(), Math::addExact);
            if (used > capacity.getOrDefault(capacityKey, 0)) {
                throw new AllocationException(
                        "Allocator exceeded approved availability for " + capacityKey);
            }
        }
        for (OrderLineDemand line : demand) {
            if (allocatedByLine.getOrDefault(line.orderLineId(), 0) != line.quantity()) {
                throw new AllocationException(
                        "Allocator did not cover line " + line.orderLineId() + " exactly");
            }
        }
    }

    private static String creationFingerprint(
            String externalOrderId,
            Destination destination,
            List<OrderLineDemand> demand,
            List<WarehouseAvailability> availability) {
        List<String> demandParts = demand.stream()
                .map(Object::toString)
                .sorted()
                .toList();
        return externalOrderId + '|' + destination + '|' + demandParts + '|'
                + canonicalAvailability(availability);
    }

    private static String canonicalAvailability(List<WarehouseAvailability> availability) {
        return availability.stream()
                .sorted(Comparator.comparing(WarehouseAvailability::warehouseId)
                        .thenComparing(WarehouseAvailability::binId)
                        .thenComparing(WarehouseAvailability::sku)
                        .thenComparingInt(WarehouseAvailability::availableQuantity))
                .map(Object::toString)
                .toList()
                .toString();
    }

    private static int distinctWarehouseCount(List<Allocation> allocations) {
        return (int) allocations.stream().map(Allocation::warehouseId).distinct().count();
    }

    private static String availabilityKey(String warehouseId, String binId, String sku) {
        return warehouseId + '|' + binId + '|' + sku;
    }

    private static Map<String, FulfilmentLine> lineMap(List<FulfilmentLine> lines) {
        Map<String, FulfilmentLine> result = new LinkedHashMap<>();
        for (FulfilmentLine line : lines) {
            result.put(line.orderLineId(), line);
        }
        return result;
    }

    private static List<FulfilmentLine> replaceLine(
            List<FulfilmentLine> lines, FulfilmentLine replacement) {
        return lines.stream()
                .map(line -> line.orderLineId().equals(replacement.orderLineId())
                        ? replacement : line)
                .toList();
    }

    private static void requireKey(String key) {
        requireText(key, "idempotencyKey/scanId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private record CommandRecord(String fingerprint, String resultId) {
    }
}
