package com.example.lld.warehouse_fulfilment_domain.model;

import com.example.lld.warehouse_fulfilment_domain.exception.InvalidScanException;
import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;
import com.example.lld.warehouse_fulfilment_domain.exception.TaskAlreadyClaimedException;

/** Immutable, atomically replaced pick-work snapshot. */
public record PickTask(
        String taskId,
        String fulfilmentId,
        String orderLineId,
        String sku,
        int quantity,
        String warehouseId,
        String binId,
        Destination destination,
        Status status,
        String workerId,
        int scannedQuantity) {

    public enum Status {
        AVAILABLE,
        CLAIMED,
        COMPLETED,
        SHORT_PICKED,
        CANCELLED
    }

    public PickTask {
        if (taskId == null || taskId.isBlank()
                || fulfilmentId == null || fulfilmentId.isBlank()
                || orderLineId == null || orderLineId.isBlank()
                || sku == null || sku.isBlank()
                || warehouseId == null || warehouseId.isBlank()
                || binId == null || binId.isBlank()) {
            throw new IllegalArgumentException("pick-task identifiers must not be blank");
        }
        if (quantity <= 0 || scannedQuantity < 0 || scannedQuantity > quantity) {
            throw new IllegalArgumentException("invalid task quantity");
        }
        if (destination == null || status == null) {
            throw new IllegalArgumentException("destination and status are required");
        }
        if (status == Status.AVAILABLE && workerId != null) {
            throw new IllegalArgumentException("an available task cannot have a worker");
        }
        if ((status == Status.CLAIMED || status == Status.COMPLETED
                || status == Status.SHORT_PICKED)
                && (workerId == null || workerId.isBlank())) {
            throw new IllegalArgumentException("claimed/terminal pick work requires a worker");
        }
        if (status == Status.COMPLETED && scannedQuantity != quantity) {
            throw new IllegalArgumentException("completed task must have its full quantity scanned");
        }
        if (status == Status.SHORT_PICKED && scannedQuantity >= quantity) {
            throw new IllegalArgumentException("short-picked task must have a short quantity");
        }
    }

    public static PickTask available(
            String taskId,
            String fulfilmentId,
            Allocation allocation,
            Destination destination) {
        return new PickTask(
                taskId, fulfilmentId, allocation.orderLineId(), allocation.sku(),
                allocation.quantity(), allocation.warehouseId(), allocation.binId(),
                destination, Status.AVAILABLE, null, 0);
    }

    public PickTask claim(String worker) {
        requireText(worker, "workerId");
        if (status == Status.CLAIMED && worker.equals(workerId)) {
            return this;
        }
        if (status == Status.CLAIMED) {
            throw new TaskAlreadyClaimedException(taskId, workerId);
        }
        if (status != Status.AVAILABLE) {
            throw new InvalidStateException(
                    "Task " + taskId + " cannot be claimed from " + status);
        }
        return copy(Status.CLAIMED, worker, scannedQuantity);
    }

    public PickTask scan(
            String worker, String scannedWarehouse, String scannedBin, String scannedSku,
            int scanQuantity) {
        requireClaimedBy(worker);
        if (!warehouseId.equals(scannedWarehouse)
                || !binId.equals(scannedBin)
                || !sku.equals(scannedSku)) {
            throw new InvalidScanException(
                    "Scan does not match task warehouse, bin, and SKU for " + taskId);
        }
        if (scanQuantity <= 0 || scannedQuantity + scanQuantity > quantity) {
            throw new InvalidScanException(
                    "Scan quantity exceeds task remainder " + remainingQuantity());
        }
        int nextScanned = scannedQuantity + scanQuantity;
        Status nextStatus = nextScanned == quantity ? Status.COMPLETED : Status.CLAIMED;
        return copy(nextStatus, workerId, nextScanned);
    }

    public PickTask closeShort(String worker) {
        requireClaimedBy(worker);
        if (scannedQuantity >= quantity) {
            throw new InvalidStateException("Completed task cannot be short-picked: " + taskId);
        }
        return copy(Status.SHORT_PICKED, workerId, scannedQuantity);
    }

    public PickTask cancel() {
        if ((status != Status.AVAILABLE && status != Status.CLAIMED
                && status != Status.SHORT_PICKED) || scannedQuantity > 0) {
            throw new InvalidStateException(
                    "Task " + taskId + " cannot be cancelled from " + status);
        }
        return copy(Status.CANCELLED, workerId, scannedQuantity);
    }

    public int remainingQuantity() {
        return quantity - scannedQuantity;
    }

    private void requireClaimedBy(String worker) {
        if (status != Status.CLAIMED || !workerId.equals(worker)) {
            throw new InvalidScanException(
                    "Task " + taskId + " is not claimed by worker " + worker);
        }
    }

    private PickTask copy(Status nextStatus, String nextWorker, int nextScanned) {
        return new PickTask(
                taskId, fulfilmentId, orderLineId, sku, quantity, warehouseId, binId,
                destination, nextStatus, nextWorker, nextScanned);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
