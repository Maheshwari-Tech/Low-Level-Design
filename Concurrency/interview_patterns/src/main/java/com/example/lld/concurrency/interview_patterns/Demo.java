package com.example.lld.concurrency.interview_patterns;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public final class Demo {
    private record SendCommand(String idempotencyKey, String recipient, String body) {}
    private record DeliveryAccepted(String idempotencyKey, String providerId, Instant occurredAt) {}

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService observers = Executors.newFixedThreadPool(2);
        EventBus eventBus = new EventBus(observers);
        AtomicInteger providerCalls = new AtomicInteger();
        List<DeliveryAccepted> observed = new CopyOnWriteArrayList<>();
        AutoCloseable subscription = eventBus.subscribe(DeliveryAccepted.class, observed::add);

        RetryPolicy policy = new RetryPolicy(
                3,
                Duration.ofMillis(5),
                Duration.ofMillis(20),
                failure -> failure instanceof TimeoutException);

        try (BoundedAsyncDispatcher<SendCommand, String> dispatcher =
                     new BoundedAsyncDispatcher<>(16, 2, command ->
                             AsyncRetry.execute(
                                     () -> simulatedProvider(command, providerCalls),
                                     policy,
                                     scheduler)
                                     .thenCompose(providerId -> eventBus.publish(
                                                     new DeliveryAccepted(command.idempotencyKey(),
                                                             providerId, Instant.now()))
                                             .thenApply(ignored -> providerId)))) {
            String providerId = dispatcher.submit(
                    new SendCommand("order-7:confirmed", "ada@example.com", "Order confirmed"))
                    .toCompletableFuture()
                    .join();

            require(providerCalls.get() == 2, "one timeout must cause exactly one retry");
            require("provider-message-1".equals(providerId), "unexpected provider id");
            require(observed.size() == 1, "observer must receive one accepted event");
            require(dispatcher.awaitDrained(Duration.ofSeconds(1)), "dispatcher did not drain");
            System.out.println("Concurrency interview patterns demo passed");
            System.out.println("  non-blocking attempts: " + providerCalls.get());
            System.out.println("  observed events: " + observed.size());
        } finally {
            subscription.close();
            scheduler.shutdown();
            observers.shutdown();
        }
    }

    private static CompletionStage<String> simulatedProvider(
            SendCommand command,
            AtomicInteger providerCalls) {
        int attempt = providerCalls.incrementAndGet();
        if (attempt == 1) {
            return CompletableFuture.failedFuture(new TimeoutException(
                    "unknown outcome for " + command.idempotencyKey()));
        }
        return CompletableFuture.completedFuture("provider-message-1");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
