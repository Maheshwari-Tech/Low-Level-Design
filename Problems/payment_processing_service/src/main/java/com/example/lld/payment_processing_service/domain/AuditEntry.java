package com.example.lld.payment_processing_service.domain;

import java.time.Instant;
import java.util.Objects;

public record AuditEntry(long sequence, Instant occurredAt, String event, String detail) {
    public AuditEntry {
        if (sequence <= 0) {
            throw new IllegalArgumentException("Audit sequence must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(detail, "detail");
    }
}
