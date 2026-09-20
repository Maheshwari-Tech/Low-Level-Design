package com.example.lld.notification_framework;

import java.util.Objects;

public record ProviderResponse(String providerMessageId, boolean deliveredSynchronously) {
    public ProviderResponse {
        if (Objects.requireNonNull(providerMessageId, "providerMessageId").isBlank()) {
            throw new IllegalArgumentException("Provider message ID cannot be blank");
        }
    }

    public static ProviderResponse delivered(String providerMessageId) {
        return new ProviderResponse(providerMessageId, true);
    }

    public static ProviderResponse accepted(String providerMessageId) {
        return new ProviderResponse(providerMessageId, false);
    }
}
