package com.example.lld.payment_processing_service.exception;

public final class InvalidPaymentStateException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
