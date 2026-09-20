package com.example.lld.payment_processing_service.provider;

import com.example.lld.payment_processing_service.domain.Money;
import com.example.lld.payment_processing_service.domain.PaymentOperation;
import java.util.Objects;
import java.util.Optional;

/** Normalised callback delivered by a provider adapter. */
public record ProviderCallback(
        String callbackId,
        String providerName,
        String providerRequestId,
        String paymentId,
        PaymentOperation operation,
        Money amount,
        ProviderOutcome outcome,
        Optional<String> providerReference,
        String detail) {
    public ProviderCallback {
        Objects.requireNonNull(callbackId, "callbackId");
        Objects.requireNonNull(providerName, "providerName");
        Objects.requireNonNull(providerRequestId, "providerRequestId");
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(outcome, "outcome");
        providerReference = Objects.requireNonNull(providerReference, "providerReference");
        detail = Objects.requireNonNull(detail, "detail");
        if (outcome == ProviderOutcome.UNKNOWN) {
            throw new IllegalArgumentException("A callback must contain a terminal outcome");
        }
    }
}
