package com.example.lld.inventory_reservation_service.model;

/** A concrete location allocation for a requested SKU. */
public record ReservationLine(String sku, String location, int quantity) {
    public ReservationLine {
        if (sku == null || sku.isBlank() || location == null || location.isBlank()) {
            throw new IllegalArgumentException("sku and location must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public InventoryKey inventoryKey() {
        return new InventoryKey(sku, location);
    }
}
