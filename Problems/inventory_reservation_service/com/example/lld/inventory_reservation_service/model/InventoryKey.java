package com.example.lld.inventory_reservation_service.model;

/** Identifies one logical stock balance. */
public record InventoryKey(String sku, String location) implements Comparable<InventoryKey> {
    public InventoryKey {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
    }

    @Override
    public int compareTo(InventoryKey other) {
        int bySku = sku.compareTo(other.sku);
        return bySku != 0 ? bySku : location.compareTo(other.location);
    }
}
