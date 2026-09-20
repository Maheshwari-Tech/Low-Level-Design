package com.example.lld.payment_processing_service.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable view of one provider interaction. */
public record PaymentAttempt(
        String attemptId,
        String providerRequestId,
        PaymentOperation operation,
        Money amount,
        AttemptStatus status,
        Optional<String> providerReference,
        String detail,
        Instant startedAt,
        Instant updatedAt) {
    public PaymentAttempt {
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(providerRequestId, "providerRequestId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(status, "status");
        providerReference = Objects.requireNonNull(providerReference, "providerReference");
        detail = Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
