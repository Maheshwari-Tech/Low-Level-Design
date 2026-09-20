package com.example.lld.notification_framework;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** A daily local-time interval; overnight ranges such as 22:00-07:00 are supported. */
public record QuietHours(LocalTime start, LocalTime end) {
    public QuietHours {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.equals(end)) {
            throw new IllegalArgumentException("Quiet-hours start and end cannot be equal");
        }
    }

    public boolean contains(Instant instant, ZoneId zoneId) {
        LocalTime localTime = Objects.requireNonNull(instant, "instant")
                .atZone(Objects.requireNonNull(zoneId, "zoneId"))
                .toLocalTime();
        if (start.isBefore(end)) {
            return !localTime.isBefore(start) && localTime.isBefore(end);
        }
        return !localTime.isBefore(start) || localTime.isBefore(end);
    }
}
