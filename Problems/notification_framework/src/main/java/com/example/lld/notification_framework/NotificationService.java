package com.example.lld.notification_framework;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory reference implementation. All state transitions are synchronized; a durable version
 * would use row claims/outbox records under the same transition boundaries.
 */
public final class NotificationService {
    public enum CallbackResult {
        APPLIED,
        DUPLICATE,
        IGNORED_STALE
    }

    private record StoredRequest(NotificationRequest request, String notificationId) {
    }

    private record ProviderMessageKey(String providerName, String providerMessageId) {
    }

    private record CallbackPointer(
            String notificationId, DeliveryWork delivery, int attemptIndex, long attemptId) {
    }

    private static final class DeliveryWork {
        private final Recipient recipient;
        private final Channel channel;
        private final String destination;
        private final NotificationTemplate template;
        private final RenderedMessage renderedMessage;
        private NotificationView.DeliveryStatus status;
        private String detail;
        private Instant nextAttemptAt;
        private int providerIndex;
        private int attemptsOnProvider;
        private long latestAttemptId = -1L;

        private DeliveryWork(
                Recipient recipient,
                Channel channel,
                String destination,
                NotificationTemplate template,
                RenderedMessage renderedMessage,
                NotificationView.DeliveryStatus status,
                String detail,
                Instant nextAttemptAt) {
            this.recipient = recipient;
            this.channel = channel;
            this.destination = destination;
            this.template = template;
            this.renderedMessage = renderedMessage;
            this.status = status;
            this.detail = detail;
            this.nextAttemptAt = nextAttemptAt;
        }

        private boolean isDue(Instant now) {
            return (status == NotificationView.DeliveryStatus.PENDING
                    || status == NotificationView.DeliveryStatus.RETRY_SCHEDULED)
                    && nextAttemptAt != null
                    && !nextAttemptAt.isAfter(now);
        }
    }

    private static final class MutableNotification {
        private final String id;
        private final NotificationRequest request;
        private final Instant createdAt;
        private final List<DeliveryWork> deliveries = new ArrayList<>();
        private final List<NotificationView.DeliveryAttempt> attempts = new ArrayList<>();
        private final List<NotificationView.AuditEvent> audit = new ArrayList<>();

        private MutableNotification(String id, NotificationRequest request, Instant createdAt) {
            this.id = id;
            this.request = request;
            this.createdAt = createdAt;
        }
    }

    private final Clock clock;
    private final TemplateRepository templates;
    private final RetryPolicy retryPolicy;
    private final Map<Channel, List<ChannelProvider>> providers;
    private final Map<String, StoredRequest> requestsByIdempotencyKey = new HashMap<>();
    private final Map<String, MutableNotification> notifications = new LinkedHashMap<>();
    private final Map<ProviderMessageKey, CallbackPointer> callbackPointers = new HashMap<>();
    private final Set<String> processedCallbackEvents = new HashSet<>();
    private long notificationSequence;
    private long attemptSequence;

    public NotificationService(
            Clock clock,
            TemplateRepository templates,
            RetryPolicy retryPolicy,
            Map<Channel, List<ChannelProvider>> providerChains) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.templates = Objects.requireNonNull(templates, "templates");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(providerChains, "providerChains");

        Map<Channel, List<ChannelProvider>> copied = new HashMap<>();
        Set<String> providerNames = new HashSet<>();
        for (Map.Entry<Channel, List<ChannelProvider>> entry : providerChains.entrySet()) {
            List<ChannelProvider> chain = List.copyOf(entry.getValue());
            for (ChannelProvider provider : chain) {
                if (provider.channel() != entry.getKey()) {
                    throw new IllegalArgumentException(
                            "Provider " + provider.name() + " is registered under the wrong channel");
                }
                if (provider.name().isBlank() || !providerNames.add(provider.name())) {
                    throw new IllegalArgumentException(
                            "Provider names must be non-blank and globally unique: " + provider.name());
                }
            }
            copied.put(entry.getKey(), chain);
        }
        providers = Map.copyOf(copied);
    }

    /** Returns the existing logical ID for an exact idempotent replay. */
    public synchronized String submit(NotificationRequest request) {
        Objects.requireNonNull(request, "request");
        StoredRequest existing = requestsByIdempotencyKey.get(request.idempotencyKey());
        if (existing != null) {
            if (!existing.request().equals(request)) {
                throw new NotificationException.IdempotencyConflictException(
                        "Idempotency key was reused with a different request: "
                                + request.idempotencyKey());
            }
            return existing.notificationId();
        }

        Instant now = clock.instant();
        String notificationId = String.format(
                Locale.ROOT, "notification-%04d", ++notificationSequence);
        MutableNotification notification = new MutableNotification(notificationId, request, now);
        for (Recipient recipient : request.recipients()) {
            for (Channel channel : recipient.preferences().order(request.channels())) {
                DeliveryWork delivery = prepareDelivery(request, recipient, channel, now);
                notification.deliveries.add(delivery);
                if (delivery.status == NotificationView.DeliveryStatus.SUPPRESSED
                        || delivery.status == NotificationView.DeliveryStatus.FAILED) {
                    audit(notification, now, delivery.status.name(),
                            recipient.id() + "/" + channel + ": " + delivery.detail);
                }
            }
        }
        audit(notification, now, "ACCEPTED", "Request accepted with key " + request.idempotencyKey());
        notifications.put(notificationId, notification);
        requestsByIdempotencyKey.put(
                request.idempotencyKey(), new StoredRequest(request, notificationId));
        return notificationId;
    }

    public synchronized int processDue() {
        return processDue(clock.instant());
    }

    /** Processes all work due at or before now, including an immediately selected fallback. */
    public synchronized int processDue(Instant now) {
        Objects.requireNonNull(now, "now");
        int processed = 0;
        while (true) {
            MutableNotification owner = null;
            DeliveryWork due = null;
            for (MutableNotification candidate : notifications.values()) {
                for (DeliveryWork delivery : candidate.deliveries) {
                    if (delivery.isDue(now)) {
                        owner = candidate;
                        due = delivery;
                        break;
                    }
                }
                if (due != null) {
                    break;
                }
            }
            if (due == null || owner == null) {
                return processed;
            }
            sendAttempt(owner, due, now);
            processed++;
        }
    }

    public synchronized CallbackResult handleCallback(ProviderCallback callback) {
        Objects.requireNonNull(callback, "callback");
        ProviderMessageKey key = new ProviderMessageKey(
                callback.providerName(), callback.providerMessageId());
        CallbackPointer pointer = callbackPointers.get(key);
        if (pointer == null) {
            throw new NotificationException(
                    "Callback references an unknown provider message: " + key);
        }
        if (!processedCallbackEvents.add(callback.eventId())) {
            return CallbackResult.DUPLICATE;
        }

        MutableNotification notification = notifications.get(pointer.notificationId());
        DeliveryWork delivery = pointer.delivery();
        if (delivery.status != NotificationView.DeliveryStatus.AWAITING_CALLBACK
                || delivery.latestAttemptId != pointer.attemptId()) {
            audit(notification, callback.occurredAt(), "CALLBACK_IGNORED",
                    "Stale terminal callback " + callback.eventId());
            return CallbackResult.IGNORED_STALE;
        }

        NotificationView.DeliveryAttempt attempt = notification.attempts.get(pointer.attemptIndex());
        switch (callback.state()) {
            case DELIVERED -> {
                notification.attempts.set(pointer.attemptIndex(), attempt.withCallbackState(
                        NotificationView.AttemptState.DELIVERED,
                        callback.occurredAt(),
                        callback.detail()));
                delivery.status = NotificationView.DeliveryStatus.DELIVERED;
                delivery.detail = callback.detail();
                delivery.nextAttemptAt = null;
            }
            case TRANSIENT_FAILURE -> {
                notification.attempts.set(pointer.attemptIndex(), attempt.withCallbackState(
                        NotificationView.AttemptState.TRANSIENT_FAILED,
                        callback.occurredAt(),
                        callback.detail()));
                scheduleAfterTransient(notification, delivery, callback.occurredAt(), callback.detail());
            }
            case PERMANENT_FAILURE -> {
                notification.attempts.set(pointer.attemptIndex(), attempt.withCallbackState(
                        NotificationView.AttemptState.PERMANENT_FAILED,
                        callback.occurredAt(),
                        callback.detail()));
                delivery.status = NotificationView.DeliveryStatus.FAILED;
                delivery.detail = callback.detail();
                delivery.nextAttemptAt = null;
            }
        }
        audit(notification, callback.occurredAt(), "CALLBACK_" + callback.state(),
                callback.providerName() + ": " + callback.detail());
        return CallbackResult.APPLIED;
    }

    public synchronized NotificationView status(String notificationId) {
        MutableNotification notification = notifications.get(notificationId);
        if (notification == null) {
            throw new NotificationException("Unknown notification: " + notificationId);
        }
        List<NotificationView.Delivery> deliveryViews = notification.deliveries.stream()
                .map(delivery -> new NotificationView.Delivery(
                        delivery.recipient.id(),
                        delivery.channel,
                        delivery.status,
                        delivery.detail,
                        Optional.ofNullable(delivery.nextAttemptAt)))
                .toList();
        return new NotificationView(
                notification.id,
                aggregateStatus(notification),
                notification.request,
                notification.createdAt,
                deliveryViews,
                notification.attempts,
                notification.audit);
    }

    private DeliveryWork prepareDelivery(
            NotificationRequest request, Recipient recipient, Channel channel, Instant now) {
        if (!recipient.preferences().isOptedIn(channel)) {
            return terminalDelivery(recipient, channel, NotificationView.DeliveryStatus.SUPPRESSED,
                    "Recipient opted out");
        }
        if (recipient.preferences().isQuiet(now, recipient.zoneId())) {
            return terminalDelivery(recipient, channel, NotificationView.DeliveryStatus.SUPPRESSED,
                    "Suppressed during recipient quiet hours");
        }
        String destination = recipient.destinations().get(channel);
        if (destination == null) {
            return terminalDelivery(recipient, channel, NotificationView.DeliveryStatus.SUPPRESSED,
                    "No destination configured");
        }
        if (providers.getOrDefault(channel, List.of()).isEmpty()) {
            return terminalDelivery(recipient, channel, NotificationView.DeliveryStatus.FAILED,
                    "No provider configured");
        }
        try {
            NotificationTemplate template = templates.resolve(
                    request.notificationType(), request.templateVersion(), recipient.locale(), channel);
            RenderedMessage rendered = template.render(request.templateData());
            return new DeliveryWork(
                    recipient,
                    channel,
                    destination,
                    template,
                    rendered,
                    NotificationView.DeliveryStatus.PENDING,
                    "Ready for delivery",
                    now);
        } catch (NotificationException exception) {
            return terminalDelivery(recipient, channel, NotificationView.DeliveryStatus.FAILED,
                    exception.getMessage());
        }
    }

    private static DeliveryWork terminalDelivery(
            Recipient recipient,
            Channel channel,
            NotificationView.DeliveryStatus status,
            String detail) {
        return new DeliveryWork(recipient, channel, "", null, null, status, detail, null);
    }

    private void sendAttempt(
            MutableNotification notification, DeliveryWork delivery, Instant now) {
        List<ChannelProvider> chain = providers.get(delivery.channel);
        ChannelProvider provider = chain.get(delivery.providerIndex);
        int providerAttempt = ++delivery.attemptsOnProvider;
        long attemptId = ++attemptSequence;
        Instant scheduledAt = delivery.nextAttemptAt;
        delivery.latestAttemptId = attemptId;

        try {
            ProviderResponse response = provider.send(new ProviderMessage(
                    notification.id,
                    attemptId,
                    delivery.recipient.id(),
                    delivery.destination,
                    delivery.channel,
                    notification.request.notificationType(),
                    delivery.template.version(),
                    delivery.template.locale(),
                    delivery.renderedMessage));
            ProviderMessageKey messageKey = new ProviderMessageKey(
                    provider.name(), response.providerMessageId());
            if (callbackPointers.containsKey(messageKey)) {
                recordFailure(notification, delivery, provider, providerAttempt, attemptId,
                        scheduledAt, now, FailureType.PERMANENT,
                        "Provider returned a duplicate message ID");
                return;
            }

            NotificationView.AttemptState attemptState = response.deliveredSynchronously()
                    ? NotificationView.AttemptState.DELIVERED
                    : NotificationView.AttemptState.ACCEPTED;
            int attemptIndex = notification.attempts.size();
            notification.attempts.add(new NotificationView.DeliveryAttempt(
                    attemptId,
                    delivery.recipient.id(),
                    delivery.channel,
                    provider.name(),
                    providerAttempt,
                    delivery.template.version(),
                    delivery.template.locale(),
                    delivery.renderedMessage,
                    attemptState,
                    scheduledAt,
                    now,
                    now,
                    Optional.of(response.providerMessageId()),
                    response.deliveredSynchronously() ? "Delivered" : "Accepted by provider"));
            callbackPointers.put(messageKey, new CallbackPointer(
                    notification.id, delivery, attemptIndex, attemptId));
            delivery.status = response.deliveredSynchronously()
                    ? NotificationView.DeliveryStatus.DELIVERED
                    : NotificationView.DeliveryStatus.AWAITING_CALLBACK;
            delivery.detail = response.deliveredSynchronously()
                    ? "Delivered by " + provider.name()
                    : "Awaiting callback from " + provider.name();
            delivery.nextAttemptAt = null;
            audit(notification, now, attemptState.name(),
                    delivery.recipient.id() + "/" + delivery.channel + " via " + provider.name());
        } catch (ProviderFailureException exception) {
            recordFailure(notification, delivery, provider, providerAttempt, attemptId,
                    scheduledAt, now, exception.failureType(), exception.getMessage());
        } catch (RuntimeException exception) {
            recordFailure(notification, delivery, provider, providerAttempt, attemptId,
                    scheduledAt, now, FailureType.TRANSIENT,
                    "Unexpected provider exception: " + exception.getClass().getSimpleName());
        }
    }

    private void recordFailure(
            MutableNotification notification,
            DeliveryWork delivery,
            ChannelProvider provider,
            int providerAttempt,
            long attemptId,
            Instant scheduledAt,
            Instant now,
            FailureType failureType,
            String detail) {
        NotificationView.AttemptState state = failureType == FailureType.TRANSIENT
                ? NotificationView.AttemptState.TRANSIENT_FAILED
                : NotificationView.AttemptState.PERMANENT_FAILED;
        notification.attempts.add(new NotificationView.DeliveryAttempt(
                attemptId,
                delivery.recipient.id(),
                delivery.channel,
                provider.name(),
                providerAttempt,
                delivery.template.version(),
                delivery.template.locale(),
                delivery.renderedMessage,
                state,
                scheduledAt,
                now,
                now,
                Optional.empty(),
                detail));
        audit(notification, now, state.name(), provider.name() + ": " + detail);
        if (failureType == FailureType.TRANSIENT) {
            scheduleAfterTransient(notification, delivery, now, detail);
        } else {
            delivery.status = NotificationView.DeliveryStatus.FAILED;
            delivery.detail = detail;
            delivery.nextAttemptAt = null;
        }
    }

    private void scheduleAfterTransient(
            MutableNotification notification, DeliveryWork delivery, Instant now, String detail) {
        if (delivery.attemptsOnProvider < retryPolicy.maxAttemptsPerProvider()) {
            delivery.status = NotificationView.DeliveryStatus.RETRY_SCHEDULED;
            delivery.nextAttemptAt = now.plus(
                    retryPolicy.delayAfterFailure(delivery.attemptsOnProvider));
            delivery.detail = "Retry scheduled after transient failure: " + detail;
            audit(notification, now, "RETRY_SCHEDULED", delivery.nextAttemptAt.toString());
            return;
        }

        List<ChannelProvider> chain = providers.get(delivery.channel);
        if (delivery.providerIndex + 1 < chain.size()) {
            String exhaustedProvider = chain.get(delivery.providerIndex).name();
            delivery.providerIndex++;
            delivery.attemptsOnProvider = 0;
            delivery.status = NotificationView.DeliveryStatus.PENDING;
            delivery.nextAttemptAt = now;
            delivery.detail = "Falling back after " + exhaustedProvider;
            audit(notification, now, "PROVIDER_FALLBACK",
                    exhaustedProvider + " -> " + chain.get(delivery.providerIndex).name());
        } else {
            delivery.status = NotificationView.DeliveryStatus.FAILED;
            delivery.detail = "Transient retries exhausted: " + detail;
            delivery.nextAttemptAt = null;
        }
    }

    private static NotificationView.Status aggregateStatus(MutableNotification notification) {
        int delivered = 0;
        int failed = 0;
        int suppressed = 0;
        int active = 0;
        for (DeliveryWork delivery : notification.deliveries) {
            switch (delivery.status) {
                case DELIVERED -> delivered++;
                case FAILED -> failed++;
                case SUPPRESSED -> suppressed++;
                case PENDING, RETRY_SCHEDULED, AWAITING_CALLBACK -> active++;
            }
        }
        if (active > 0) {
            return notification.attempts.isEmpty()
                    ? NotificationView.Status.ACCEPTED
                    : NotificationView.Status.PROCESSING;
        }
        if (delivered == notification.deliveries.size()) {
            return NotificationView.Status.DELIVERED;
        }
        if (delivered > 0) {
            return NotificationView.Status.PARTIALLY_DELIVERED;
        }
        if (suppressed == notification.deliveries.size()) {
            return NotificationView.Status.SUPPRESSED;
        }
        if (failed > 0) {
            return NotificationView.Status.FAILED;
        }
        throw new IllegalStateException("Notification has no aggregate state");
    }

    private static void audit(
            MutableNotification notification, Instant at, String action, String detail) {
        notification.audit.add(new NotificationView.AuditEvent(at, action, detail));
    }
}
