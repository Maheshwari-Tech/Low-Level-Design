package com.example.lld.inventory_reservation_service.exception;

public final class IdempotencyConflictException extends InventoryDomainException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String key) {
        super("Idempotency key was already used for different input: " + key);
    }
}
