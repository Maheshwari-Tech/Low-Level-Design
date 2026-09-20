package com.example.lld.notification_framework;

import java.time.Instant;
import java.util.Objects;

public record ProviderCallback(
        String eventId,
        String providerName,
        String providerMessageId,
        State state,
        String detail,
        Instant occurredAt) {

    public enum State {
        DELIVERED,
        TRANSIENT_FAILURE,
        PERMANENT_FAILURE
    }

    public ProviderCallback {
        requireNonBlank(eventId, "eventId");
        requireNonBlank(providerName, "providerName");
        requireNonBlank(providerMessageId, "providerMessageId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    private static void requireNonBlank(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
