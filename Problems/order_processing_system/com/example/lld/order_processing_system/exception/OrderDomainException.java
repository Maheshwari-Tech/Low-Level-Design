package com.example.lld.order_processing_system.exception;

/** Base type for failures callers may map to stable application/API error codes. */
public class OrderDomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OrderDomainException(String message) {
        super(message);
    }

    public OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
