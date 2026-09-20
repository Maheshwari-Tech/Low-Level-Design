package com.example.lld.warehouse_fulfilment_domain.model;

/** Approved physical allocation input; this domain does not own sellable inventory. */
public record WarehouseAvailability(
        String warehouseId, String binId, String sku, int availableQuantity) {
    public WarehouseAvailability {
        if (warehouseId == null || warehouseId.isBlank()
                || binId == null || binId.isBlank()
                || sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("warehouseId, binId, and sku must not be blank");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must be non-negative");
        }
    }
}
