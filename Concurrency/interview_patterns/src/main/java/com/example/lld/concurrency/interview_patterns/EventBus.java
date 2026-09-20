package com.example.lld.concurrency.interview_patterns;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** In-process asynchronous Observer. This is not a durable message broker or outbox. */
public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> subscribers =
            new ConcurrentHashMap<>();
    private final Executor executor;

    public EventBus(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public <E> AutoCloseable subscribe(Class<E> eventType, Consumer<? super E> subscriber) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(subscriber, "subscriber");
        CopyOnWriteArrayList<Consumer<?>> handlers =
                subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
        handlers.add(subscriber);
        return () -> handlers.remove(subscriber);
    }

    public CompletionStage<Void> publish(Object event) {
        Objects.requireNonNull(event, "event");
        List<CompletableFuture<Void>> calls = subscribers
                .getOrDefault(event.getClass(), new CopyOnWriteArrayList<>())
                .stream()
                .map(raw -> CompletableFuture.runAsync(() -> invoke(raw, event), executor))
                .toList();
        return CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("unchecked")
    private static <E> void invoke(Consumer<?> raw, E event) {
        ((Consumer<? super E>) raw).accept(event);
    }
}
