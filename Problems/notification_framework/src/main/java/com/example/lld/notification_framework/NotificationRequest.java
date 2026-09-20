package com.example.lld.notification_framework;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record NotificationRequest(
        String notificationType,
        int templateVersion,
        List<Recipient> recipients,
        Map<String, String> templateData,
        Set<Channel> channels,
        Priority priority,
        String idempotencyKey) {

    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }

    public NotificationRequest {
        if (Objects.requireNonNull(notificationType, "notificationType").isBlank()) {
            throw new IllegalArgumentException("Notification type cannot be blank");
        }
        if (templateVersion <= 0) {
            throw new IllegalArgumentException("Template version must be positive");
        }
        recipients = List.copyOf(Objects.requireNonNull(recipients, "recipients"));
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        Set<String> recipientIds = new HashSet<>();
        for (Recipient recipient : recipients) {
            if (!recipientIds.add(recipient.id())) {
                throw new IllegalArgumentException("Duplicate recipient: " + recipient.id());
            }
        }
        templateData = Map.copyOf(Objects.requireNonNull(templateData, "templateData"));
        channels = Set.copyOf(Objects.requireNonNull(channels, "channels"));
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("At least one channel is required");
        }
        Objects.requireNonNull(priority, "priority");
        if (Objects.requireNonNull(idempotencyKey, "idempotencyKey").isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
    }
}
