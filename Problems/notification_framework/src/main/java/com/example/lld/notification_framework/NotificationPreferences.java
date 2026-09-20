package com.example.lld.notification_framework;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record NotificationPreferences(
        Map<Channel, Boolean> optedIn,
        List<Channel> preferredChannels,
        Optional<QuietHours> quietHours) {

    public NotificationPreferences {
        optedIn = Map.copyOf(Objects.requireNonNull(optedIn, "optedIn"));
        preferredChannels = List.copyOf(
                Objects.requireNonNull(preferredChannels, "preferredChannels"));
        if (new LinkedHashSet<>(preferredChannels).size() != preferredChannels.size()) {
            throw new IllegalArgumentException("Preferred channels cannot contain duplicates");
        }
        Objects.requireNonNull(quietHours, "quietHours");
    }

    public boolean isOptedIn(Channel channel) {
        return optedIn.getOrDefault(channel, false);
    }

    public boolean isQuiet(Instant now, ZoneId zoneId) {
        return quietHours.map(hours -> hours.contains(now, zoneId)).orElse(false);
    }

    public List<Channel> order(Set<Channel> requested) {
        Objects.requireNonNull(requested, "requested");
        List<Channel> ordered = new ArrayList<>();
        for (Channel channel : preferredChannels) {
            if (requested.contains(channel)) {
                ordered.add(channel);
            }
        }
        requested.stream().sorted().filter(channel -> !ordered.contains(channel)).forEach(ordered::add);
        return List.copyOf(ordered);
    }
}
