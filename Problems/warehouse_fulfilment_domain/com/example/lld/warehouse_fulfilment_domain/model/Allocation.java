package com.example.lld.warehouse_fulfilment_domain.model;

/** A strategy-selected source for part or all of one order-line demand. */
public record Allocation(
        String orderLineId, String sku, String warehouseId, String binId, int quantity) {
    public Allocation {
        if (orderLineId == null || orderLineId.isBlank()
                || sku == null || sku.isBlank()
                || warehouseId == null || warehouseId.isBlank()
                || binId == null || binId.isBlank()) {
            throw new IllegalArgumentException("allocation identifiers must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
