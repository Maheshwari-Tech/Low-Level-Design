package com.example.lld.notification_framework;

import java.util.Locale;
import java.util.Objects;

public record ProviderMessage(
        String notificationId,
        long attemptId,
        String recipientId,
        String destination,
        Channel channel,
        String notificationType,
        int templateVersion,
        Locale templateLocale,
        RenderedMessage content) {

    public ProviderMessage {
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(notificationType, "notificationType");
        Objects.requireNonNull(templateLocale, "templateLocale");
        Objects.requireNonNull(content, "content");
    }
}
