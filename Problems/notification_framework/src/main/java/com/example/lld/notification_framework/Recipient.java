package com.example.lld.notification_framework;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record Recipient(
        String id,
        Locale locale,
        ZoneId zoneId,
        Map<Channel, String> destinations,
        NotificationPreferences preferences) {

    public Recipient {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("Recipient ID cannot be blank");
        }
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(zoneId, "zoneId");
        destinations = Map.copyOf(Objects.requireNonNull(destinations, "destinations"));
        for (Map.Entry<Channel, String> entry : destinations.entrySet()) {
            if (entry.getValue().isBlank()) {
                throw new IllegalArgumentException(
                        "Destination cannot be blank for " + entry.getKey());
            }
        }
        Objects.requireNonNull(preferences, "preferences");
    }
}
