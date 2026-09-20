package com.example.lld.payment_processing_service.exception;

public final class IdempotencyConflictException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String key) {
        super("Idempotency key was already used for a different command: " + key);
    }
}
