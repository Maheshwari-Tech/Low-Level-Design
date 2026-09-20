package com.example.lld.order_processing_system.exception;

public final class InvalidOrderStateException extends OrderDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
