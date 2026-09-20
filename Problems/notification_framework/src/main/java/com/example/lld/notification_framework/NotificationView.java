package com.example.lld.notification_framework;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record NotificationView(
        String notificationId,
        Status status,
        NotificationRequest request,
        Instant createdAt,
        List<Delivery> deliveries,
        List<DeliveryAttempt> attempts,
        List<AuditEvent> auditTrail) {

    public enum Status {
        ACCEPTED,
        PROCESSING,
        DELIVERED,
        PARTIALLY_DELIVERED,
        FAILED,
        SUPPRESSED
    }

    public enum DeliveryStatus {
        PENDING,
        RETRY_SCHEDULED,
        AWAITING_CALLBACK,
        DELIVERED,
        FAILED,
        SUPPRESSED
    }

    public enum AttemptState {
        IN_FLIGHT,
        ACCEPTED,
        DELIVERED,
        TRANSIENT_FAILED,
        PERMANENT_FAILED
    }

    public record Delivery(
            String recipientId,
            Channel channel,
            DeliveryStatus status,
            String detail,
            Optional<Instant> nextAttemptAt) {
        public Delivery {
            Objects.requireNonNull(recipientId, "recipientId");
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        }
    }

    public record DeliveryAttempt(
            long attemptId,
            String recipientId,
            Channel channel,
            String provider,
            int providerAttemptNumber,
            int templateVersion,
            Locale templateLocale,
            RenderedMessage renderedMessage,
            AttemptState state,
            Instant scheduledAt,
            Instant startedAt,
            Instant endedAt,
            Optional<String> providerMessageId,
            String detail) {
        public DeliveryAttempt {
            Objects.requireNonNull(recipientId, "recipientId");
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(templateLocale, "templateLocale");
            Objects.requireNonNull(renderedMessage, "renderedMessage");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(scheduledAt, "scheduledAt");
            Objects.requireNonNull(startedAt, "startedAt");
            Objects.requireNonNull(endedAt, "endedAt");
            Objects.requireNonNull(providerMessageId, "providerMessageId");
            Objects.requireNonNull(detail, "detail");
        }

        public DeliveryAttempt withCallbackState(
                AttemptState callbackState, Instant callbackAt, String callbackDetail) {
            return new DeliveryAttempt(
                    attemptId,
                    recipientId,
                    channel,
                    provider,
                    providerAttemptNumber,
                    templateVersion,
                    templateLocale,
                    renderedMessage,
                    callbackState,
                    scheduledAt,
                    startedAt,
                    callbackAt,
                    providerMessageId,
                    callbackDetail);
        }
    }

    public record AuditEvent(Instant at, String action, String detail) {
        public AuditEvent {
            Objects.requireNonNull(at, "at");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(detail, "detail");
        }
    }

    public NotificationView {
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(createdAt, "createdAt");
        deliveries = List.copyOf(Objects.requireNonNull(deliveries, "deliveries"));
        attempts = List.copyOf(Objects.requireNonNull(attempts, "attempts"));
        auditTrail = List.copyOf(Objects.requireNonNull(auditTrail, "auditTrail"));
    }
}
