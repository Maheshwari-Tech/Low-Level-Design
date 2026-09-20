package com.example.lld.warehouse_fulfilment_domain.model;

import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;

/** Immutable line counters guarded by fulfilment quantity invariants. */
public record FulfilmentLine(
        String orderLineId,
        String sku,
        int requestedQuantity,
        int allocatedQuantity,
        int pickedQuantity,
        int packedQuantity,
        int shippedQuantity,
        int cancelledQuantity) {

    public FulfilmentLine {
        if (orderLineId == null || orderLineId.isBlank() || sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("orderLineId and sku must not be blank");
        }
        if (requestedQuantity <= 0
                || allocatedQuantity < 0
                || pickedQuantity < 0
                || packedQuantity < 0
                || shippedQuantity < 0
                || cancelledQuantity < 0
                || shippedQuantity > packedQuantity
                || packedQuantity > pickedQuantity
                || pickedQuantity > allocatedQuantity
                || allocatedQuantity + cancelledQuantity > requestedQuantity) {
            throw new IllegalArgumentException("fulfilment line quantity invariant violated");
        }
    }

    public static FulfilmentLine from(OrderLineDemand demand) {
        return new FulfilmentLine(
                demand.orderLineId(), demand.sku(), demand.quantity(), 0, 0, 0, 0, 0);
    }

    public FulfilmentLine allocate(int quantity) {
        if (quantity <= 0 || allocatedQuantity + cancelledQuantity + quantity > requestedQuantity) {
            throw invalid("allocate", quantity);
        }
        return copy(allocatedQuantity + quantity, pickedQuantity, packedQuantity,
                shippedQuantity, cancelledQuantity);
    }

    public FulfilmentLine recordPick(int quantity) {
        if (quantity <= 0 || pickedQuantity + quantity > allocatedQuantity) {
            throw invalid("pick", quantity);
        }
        return copy(allocatedQuantity, pickedQuantity + quantity, packedQuantity,
                shippedQuantity, cancelledQuantity);
    }

    /** Removes the unpicked part of a short allocation before it is reallocated. */
    public FulfilmentLine removeShortAllocation(int quantity) {
        if (quantity <= 0 || allocatedQuantity - quantity < pickedQuantity) {
            throw invalid("remove short allocation", quantity);
        }
        return copy(allocatedQuantity - quantity, pickedQuantity, packedQuantity,
                shippedQuantity, cancelledQuantity);
    }

    public FulfilmentLine recordPack(int quantity) {
        if (quantity <= 0 || packedQuantity + quantity > pickedQuantity) {
            throw invalid("pack", quantity);
        }
        return copy(allocatedQuantity, pickedQuantity, packedQuantity + quantity,
                shippedQuantity, cancelledQuantity);
    }

    public FulfilmentLine recordShipment(int quantity) {
        if (quantity <= 0 || shippedQuantity + quantity > packedQuantity) {
            throw invalid("ship", quantity);
        }
        return copy(allocatedQuantity, pickedQuantity, packedQuantity,
                shippedQuantity + quantity, cancelledQuantity);
    }

    public FulfilmentLine cancelUnpicked() {
        if (pickedQuantity > 0) {
            throw new InvalidStateException(
                    "Line " + orderLineId + " has picked stock and cannot be cancelled");
        }
        return copy(0, 0, 0, 0, requestedQuantity);
    }

    public int remainingToAllocate() {
        return requestedQuantity - allocatedQuantity - cancelledQuantity;
    }

    public int unpackedQuantity() {
        return pickedQuantity - packedQuantity;
    }

    public int unshippedQuantity() {
        return packedQuantity - shippedQuantity;
    }

    private FulfilmentLine copy(
            int allocated, int picked, int packed, int shipped, int cancelled) {
        return new FulfilmentLine(
                orderLineId, sku, requestedQuantity, allocated, picked, packed, shipped,
                cancelled);
    }

    private InvalidStateException invalid(String action, int quantity) {
        return new InvalidStateException(
                "Cannot " + action + " " + quantity + " unit(s) for line " + orderLineId);
    }
}
