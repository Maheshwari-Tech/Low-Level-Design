package com.example.lld.inventory_reservation_service.model;

/** Immutable read/allocation snapshot; mutable counters remain encapsulated by the service. */
public record InventoryBalance(InventoryKey key, int onHand, int reserved) {
    public InventoryBalance {
        if (key == null) {
            throw new IllegalArgumentException("key is required");
        }
        if (onHand < 0 || reserved < 0 || reserved > onHand) {
            throw new IllegalArgumentException("balance must satisfy 0 <= reserved <= onHand");
        }
    }

    public int available() {
        return onHand - reserved;
    }
}
