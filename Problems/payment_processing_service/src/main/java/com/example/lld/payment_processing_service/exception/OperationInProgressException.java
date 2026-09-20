package com.example.lld.payment_processing_service.exception;

public final class OperationInProgressException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public OperationInProgressException(String paymentId, String providerRequestId) {
        super("Payment " + paymentId + " has an unresolved provider request: "
                + providerRequestId);
    }
}
