package com.example.lld.warehouse_fulfilment_domain.model;

import java.time.Instant;

/** Immutable explanation of a successful or exceptional warehouse command. */
public record AuditEntry(
        String auditId, String action, String actor, Instant occurredAt, String details) {
    public AuditEntry {
        if (auditId == null || auditId.isBlank()
                || action == null || action.isBlank()
                || actor == null || actor.isBlank()
                || occurredAt == null) {
            throw new IllegalArgumentException("audit identity, action, actor, and time are required");
        }
        details = details == null ? "" : details;
    }
}
