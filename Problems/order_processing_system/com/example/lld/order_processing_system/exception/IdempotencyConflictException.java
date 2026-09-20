package com.example.lld.order_processing_system.exception;

public final class IdempotencyConflictException extends OrderDomainException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String key) {
        super("Idempotency key was already used for different input: " + key);
    }
}
