package com.example.lld.notification_framework;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Deterministic provider adapter used by the executable examples and tests. */
public final class ScriptedProvider implements ChannelProvider {
    public sealed interface Step permits Success, Failure {
    }

    public record Success(String providerMessageId, boolean deliveredSynchronously) implements Step {
        public Success {
            if (Objects.requireNonNull(providerMessageId, "providerMessageId").isBlank()) {
                throw new IllegalArgumentException("Provider message ID cannot be blank");
            }
        }

        public static Success delivered(String providerMessageId) {
            return new Success(providerMessageId, true);
        }

        public static Success accepted(String providerMessageId) {
            return new Success(providerMessageId, false);
        }
    }

    public record Failure(FailureType type, String detail) implements Step {
        public Failure {
            Objects.requireNonNull(type, "type");
            if (Objects.requireNonNull(detail, "detail").isBlank()) {
                throw new IllegalArgumentException("Failure detail cannot be blank");
            }
        }
    }

    private final String name;
    private final Channel channel;
    private final Deque<Step> steps;
    private int calls;

    public ScriptedProvider(String name, Channel channel, List<Step> steps) {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be blank");
        }
        this.name = name;
        this.channel = Objects.requireNonNull(channel, "channel");
        this.steps = new ArrayDeque<>(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("At least one scripted step is required");
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public synchronized ProviderResponse send(ProviderMessage message)
            throws ProviderFailureException {
        Objects.requireNonNull(message, "message");
        calls++;
        Step step = steps.pollFirst();
        if (step == null) {
            throw new ProviderFailureException(
                    FailureType.PERMANENT, "Script has no step for call " + calls);
        }
        if (step instanceof Failure failure) {
            throw new ProviderFailureException(failure.type(), failure.detail());
        }
        Success success = (Success) step;
        return success.deliveredSynchronously()
                ? ProviderResponse.delivered(success.providerMessageId())
                : ProviderResponse.accepted(success.providerMessageId());
    }

    public synchronized int calls() {
        return calls;
    }
}
