package com.example.lld.warehouse_fulfilment_domain.exception;

public final class IdempotencyConflictException extends WarehouseDomainException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String key) {
        super("Idempotency key was reused with different input: " + key);
    }
}
