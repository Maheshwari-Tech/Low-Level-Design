package com.example.lld.inventory_reservation_service.exception;

/** Base type for stable inventory-domain failures. */
public class InventoryDomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InventoryDomainException(String message) {
        super(message);
    }
}
