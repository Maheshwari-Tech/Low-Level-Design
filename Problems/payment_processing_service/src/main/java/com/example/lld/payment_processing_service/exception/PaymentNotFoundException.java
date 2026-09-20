package com.example.lld.payment_processing_service.exception;

public final class PaymentNotFoundException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public PaymentNotFoundException(String paymentId) {
        super("Payment not found: " + paymentId);
    }
}
