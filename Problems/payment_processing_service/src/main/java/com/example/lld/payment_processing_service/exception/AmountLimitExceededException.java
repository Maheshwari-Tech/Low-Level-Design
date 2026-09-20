package com.example.lld.payment_processing_service.exception;

public final class AmountLimitExceededException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public AmountLimitExceededException(String message) {
        super(message);
    }
}
