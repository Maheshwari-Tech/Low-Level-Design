package com.example.lld.payment_processing_service.exception;

public final class ProviderCallbackException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public ProviderCallbackException(String message) {
        super(message);
    }
}
