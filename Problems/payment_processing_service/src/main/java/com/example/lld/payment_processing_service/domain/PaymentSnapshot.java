package com.example.lld.payment_processing_service.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable aggregate snapshot safe to retain after later payment mutations. */
public record PaymentSnapshot(
        String paymentId,
        String orderId,
        String providerName,
        Money amount,
        Money capturedTotal,
        Money refundedTotal,
        PaymentStatus status,
        Optional<String> pendingProviderRequestId,
        List<PaymentAttempt> attempts,
        List<AuditEntry> auditHistory) {
    public PaymentSnapshot {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(providerName, "providerName");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(capturedTotal, "capturedTotal");
        Objects.requireNonNull(refundedTotal, "refundedTotal");
        Objects.requireNonNull(status, "status");
        pendingProviderRequestId =
                Objects.requireNonNull(pendingProviderRequestId, "pendingProviderRequestId");
        attempts = List.copyOf(attempts);
        auditHistory = List.copyOf(auditHistory);
    }
}
