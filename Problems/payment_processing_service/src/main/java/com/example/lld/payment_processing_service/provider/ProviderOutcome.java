package com.example.lld.payment_processing_service.provider;

/** UNKNOWN means the request timed out after its provider outcome became uncertain. */
public enum ProviderOutcome {
    APPROVED,
    DECLINED,
    TRANSIENT_FAILURE,
    UNKNOWN
}
