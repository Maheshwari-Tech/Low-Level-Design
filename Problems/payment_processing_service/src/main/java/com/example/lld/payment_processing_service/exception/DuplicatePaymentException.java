package com.example.lld.payment_processing_service.exception;

public final class DuplicatePaymentException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public DuplicatePaymentException(String paymentId) {
        super("A payment already exists with id: " + paymentId);
    }
}
