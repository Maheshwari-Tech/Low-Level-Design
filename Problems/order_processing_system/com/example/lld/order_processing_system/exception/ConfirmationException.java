package com.example.lld.order_processing_system.exception;

public final class ConfirmationException extends OrderDomainException {
    private static final long serialVersionUID = 1L;

    public ConfirmationException(String message, Throwable cause) {
        super(message, cause);
    }
}
