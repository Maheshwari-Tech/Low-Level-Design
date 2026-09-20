package com.example.lld.payment_processing_service.domain;

public enum PaymentStatus {
    CREATED,
    AUTHORIZING,
    AUTHORIZED,
    PARTIALLY_CAPTURED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    VOIDED,
    DECLINED,
    FAILED
}
