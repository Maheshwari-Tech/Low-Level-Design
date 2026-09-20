package com.example.lld.payment_processing_service.exception;

/** Base type for errors that callers can handle as payment-domain failures. */
public class PaymentDomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PaymentDomainException(String message) {
        super(message);
    }

    public PaymentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
