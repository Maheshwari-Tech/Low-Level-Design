package com.example.lld.order_processing_system.model;

import java.time.Instant;

/** Auditable domain history entry, including failed workflow steps. */
public record StatusChange(
        String action,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Instant occurredAt,
        String reason) {
    public StatusChange {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        if (fromStatus == null || toStatus == null || occurredAt == null) {
            throw new IllegalArgumentException("statuses and occurredAt are required");
        }
        reason = reason == null ? "" : reason;
    }
}
