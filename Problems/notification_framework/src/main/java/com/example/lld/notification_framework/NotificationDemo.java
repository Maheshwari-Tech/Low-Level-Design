package com.example.lld.notification_framework;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NotificationDemo {
    private static final Instant NOW = Instant.parse("2026-02-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final RetryPolicy STANDARD_RETRY =
            new RetryPolicy(3, Duration.ofSeconds(5), Duration.ofMinutes(1));

    private NotificationDemo() {
    }

    public static void main(String[] args) {
        InMemoryTemplateRepository templates = templates();
        demonstrateLocalisedEmail(templates);
        demonstrateOptOut(templates);
        demonstrateRetryWithoutSleeping(templates);
        demonstrateProviderFallback(templates);
        demonstrateRequestAndCallbackDeduplication(templates);
        demonstratePartialDelivery(templates);
        System.out.println("Notification Framework demo: all scenarios passed");
    }

    private static void demonstrateLocalisedEmail(InMemoryTemplateRepository templates) {
        ScriptedProvider email = provider(
                "email-localised", Channel.EMAIL, ScriptedProvider.Success.delivered("email-1"));
        NotificationService service = service(templates, STANDARD_RETRY, Map.of(
                Channel.EMAIL, List.of(email)));
        Recipient recipient = recipient(
                "alice",
                Locale.CANADA_FRENCH,
                Map.of(Channel.EMAIL, "alice@example.test"),
                Map.of(Channel.EMAIL, true),
                List.of(Channel.EMAIL));
        String id = service.submit(request("locale-1", List.of(recipient), Set.of(Channel.EMAIL)));
        service.processDue(NOW);
        NotificationView view = service.status(id);

        check(view.status() == NotificationView.Status.DELIVERED, "localised email should deliver");
        check(view.attempts().get(0).templateLocale().equals(Locale.FRENCH),
                "fr-CA should fall back to the French template");
        check(view.attempts().get(0).renderedMessage().body().contains("Bonjour Alice"),
                "template variables should render in the selected locale");
    }

    private static void demonstrateOptOut(InMemoryTemplateRepository templates) {
        ScriptedProvider sms = provider(
                "sms-opt-out", Channel.SMS, ScriptedProvider.Success.delivered("sms-unused"));
        NotificationService service = service(templates, STANDARD_RETRY, Map.of(
                Channel.SMS, List.of(sms)));
        Recipient recipient = recipient(
                "bob",
                Locale.ENGLISH,
                Map.of(Channel.SMS, "+15550000000"),
                Map.of(Channel.SMS, false),
                List.of(Channel.SMS));
        String id = service.submit(request("optout-1", List.of(recipient), Set.of(Channel.SMS)));

        check(service.status(id).status() == NotificationView.Status.SUPPRESSED,
                "opted-out channel should be suppressed");
        check(sms.calls() == 0, "suppressed delivery must never reach a provider");
    }

    private static void demonstrateRetryWithoutSleeping(InMemoryTemplateRepository templates) {
        ScriptedProvider email = new ScriptedProvider(
                "email-retry",
                Channel.EMAIL,
                List.of(
                        new ScriptedProvider.Failure(FailureType.TRANSIENT, "HTTP 503"),
                        ScriptedProvider.Success.delivered("email-retry-2")));
        NotificationService service = service(templates, STANDARD_RETRY, Map.of(
                Channel.EMAIL, List.of(email)));
        Recipient recipient = standardRecipient("carol", Channel.EMAIL, "carol@example.test");
        String id = service.submit(request("retry-1", List.of(recipient), Set.of(Channel.EMAIL)));

        check(service.processDue(NOW) == 1, "first attempt should run immediately");
        check(service.status(id).status() == NotificationView.Status.PROCESSING,
                "transient failure should schedule retry");
        check(service.processDue(NOW.plusSeconds(4)) == 0, "retry must respect backoff");
        check(service.processDue(NOW.plusSeconds(5)) == 1, "retry should run when due");
        check(service.status(id).status() == NotificationView.Status.DELIVERED,
                "retry should eventually deliver");
    }

    private static void demonstrateProviderFallback(InMemoryTemplateRepository templates) {
        ScriptedProvider primary = provider(
                "email-primary",
                Channel.EMAIL,
                new ScriptedProvider.Failure(FailureType.TRANSIENT, "provider unavailable"));
        ScriptedProvider fallback = provider(
                "email-fallback",
                Channel.EMAIL,
                ScriptedProvider.Success.delivered("fallback-1"));
        RetryPolicy noRetryBeforeFallback =
                new RetryPolicy(1, Duration.ofSeconds(1), Duration.ofSeconds(1));
        NotificationService service = service(templates, noRetryBeforeFallback, Map.of(
                Channel.EMAIL, List.of(primary, fallback)));
        Recipient recipient = standardRecipient("dana", Channel.EMAIL, "dana@example.test");
        String id = service.submit(request("fallback-1", List.of(recipient), Set.of(Channel.EMAIL)));

        check(service.processDue(NOW) == 2, "fallback should be attempted immediately");
        NotificationView view = service.status(id);
        check(view.status() == NotificationView.Status.DELIVERED, "fallback should deliver");
        check(view.attempts().get(1).provider().equals("email-fallback"),
                "second provider must be recorded in audit history");
    }

    private static void demonstrateRequestAndCallbackDeduplication(
            InMemoryTemplateRepository templates) {
        ScriptedProvider email = provider(
                "email-callback", Channel.EMAIL, ScriptedProvider.Success.accepted("async-1"));
        NotificationService service = service(templates, STANDARD_RETRY, Map.of(
                Channel.EMAIL, List.of(email)));
        Recipient recipient = standardRecipient("erin", Channel.EMAIL, "erin@example.test");
        NotificationRequest request = request(
                "dedupe-1", List.of(recipient), Set.of(Channel.EMAIL));
        String firstId = service.submit(request);
        String repeatedId = service.submit(request);
        check(firstId.equals(repeatedId), "duplicate request must return one logical notification");
        service.processDue(NOW);

        ProviderCallback delivered = new ProviderCallback(
                "callback-event-1",
                "email-callback",
                "async-1",
                ProviderCallback.State.DELIVERED,
                "delivered asynchronously",
                NOW.plusSeconds(2));
        check(service.handleCallback(delivered) == NotificationService.CallbackResult.APPLIED,
                "first callback should apply");
        check(service.handleCallback(delivered) == NotificationService.CallbackResult.DUPLICATE,
                "duplicate callback event should be ignored idempotently");
        ProviderCallback staleFailure = new ProviderCallback(
                "callback-event-2",
                "email-callback",
                "async-1",
                ProviderCallback.State.PERMANENT_FAILURE,
                "late failure",
                NOW.plusSeconds(3));
        check(service.handleCallback(staleFailure)
                        == NotificationService.CallbackResult.IGNORED_STALE,
                "out-of-order callback must not regress delivery");
        check(service.status(firstId).status() == NotificationView.Status.DELIVERED,
                "callback should finalize delivery");
    }

    private static void demonstratePartialDelivery(InMemoryTemplateRepository templates) {
        ScriptedProvider email = provider(
                "partial-email", Channel.EMAIL, ScriptedProvider.Success.delivered("partial-email-1"));
        ScriptedProvider sms = provider(
                "partial-sms",
                Channel.SMS,
                new ScriptedProvider.Failure(FailureType.PERMANENT, "invalid destination"));
        NotificationService service = service(templates, STANDARD_RETRY, Map.of(
                Channel.EMAIL, List.of(email),
                Channel.SMS, List.of(sms)));
        Recipient recipient = recipient(
                "frank",
                Locale.ENGLISH,
                Map.of(
                        Channel.EMAIL, "frank@example.test",
                        Channel.SMS, "+15551111111"),
                Map.of(Channel.EMAIL, true, Channel.SMS, true),
                List.of(Channel.EMAIL, Channel.SMS));
        String id = service.submit(request(
                "partial-1", List.of(recipient), Set.of(Channel.EMAIL, Channel.SMS)));
        service.processDue(NOW);

        check(service.status(id).status() == NotificationView.Status.PARTIALLY_DELIVERED,
                "one success and one permanent failure should remain visible as partial");
    }

    private static InMemoryTemplateRepository templates() {
        InMemoryTemplateRepository repository = new InMemoryTemplateRepository();
        repository.register(new NotificationTemplate(
                "ORDER_SHIPPED",
                1,
                Locale.ENGLISH,
                Channel.EMAIL,
                "Order {{orderId}} shipped",
                "Hello {{name}}, order {{orderId}} has shipped.",
                Set.of("name", "orderId")));
        repository.register(new NotificationTemplate(
                "ORDER_SHIPPED",
                1,
                Locale.FRENCH,
                Channel.EMAIL,
                "Commande {{orderId}} expédiée",
                "Bonjour {{name}}, la commande {{orderId}} est expédiée.",
                Set.of("name", "orderId")));
        repository.register(new NotificationTemplate(
                "ORDER_SHIPPED",
                1,
                Locale.ENGLISH,
                Channel.SMS,
                "",
                "Order {{orderId}} shipped for {{name}}.",
                Set.of("name", "orderId")));
        return repository;
    }

    private static NotificationService service(
            TemplateRepository templates,
            RetryPolicy retryPolicy,
            Map<Channel, List<ChannelProvider>> providers) {
        return new NotificationService(CLOCK, templates, retryPolicy, providers);
    }

    private static ScriptedProvider provider(
            String name, Channel channel, ScriptedProvider.Step step) {
        return new ScriptedProvider(name, channel, List.of(step));
    }

    private static NotificationRequest request(
            String idempotencyKey, List<Recipient> recipients, Set<Channel> channels) {
        return new NotificationRequest(
                "ORDER_SHIPPED",
                1,
                recipients,
                Map.of("name", "Alice", "orderId", "O-100"),
                channels,
                NotificationRequest.Priority.HIGH,
                idempotencyKey);
    }

    private static Recipient standardRecipient(String id, Channel channel, String destination) {
        return recipient(
                id,
                Locale.ENGLISH,
                Map.of(channel, destination),
                Map.of(channel, true),
                List.of(channel));
    }

    private static Recipient recipient(
            String id,
            Locale locale,
            Map<Channel, String> destinations,
            Map<Channel, Boolean> optedIn,
            List<Channel> preferredChannels) {
        return new Recipient(
                id,
                locale,
                ZoneId.of("UTC"),
                destinations,
                new NotificationPreferences(optedIn, preferredChannels, Optional.empty()));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
