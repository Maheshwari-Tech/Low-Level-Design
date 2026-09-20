package com.example.lld.warehouse_fulfilment_domain.model;

import java.time.Instant;

/** Outbox event published to the order-management boundary by stable event ID. */
public record FulfilmentEvent(
        String eventId,
        String fulfilmentId,
        String externalOrderId,
        FulfilmentStatus status,
        Instant occurredAt,
        String details) {
    public FulfilmentEvent {
        if (eventId == null || eventId.isBlank()
                || fulfilmentId == null || fulfilmentId.isBlank()
                || externalOrderId == null || externalOrderId.isBlank()
                || status == null || occurredAt == null) {
            throw new IllegalArgumentException("complete event identity and state are required");
        }
        details = details == null ? "" : details;
    }
}
