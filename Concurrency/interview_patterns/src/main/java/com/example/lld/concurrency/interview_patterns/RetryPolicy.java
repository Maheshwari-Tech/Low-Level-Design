package com.example.lld.concurrency.interview_patterns;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

public record RetryPolicy(
        int maxAttempts,
        Duration initialDelay,
        Duration maxDelay,
        Predicate<Throwable> retryable) {

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(maxDelay, "maxDelay");
        Objects.requireNonNull(retryable, "retryable");
        if (initialDelay.isNegative() || maxDelay.isNegative()
                || initialDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("invalid retry delays");
        }
    }

    public Duration delayBeforeAttempt(int nextAttempt) {
        if (nextAttempt <= 1) {
            return Duration.ZERO;
        }
        long multiplier = 1L << Math.min(30, nextAttempt - 2);
        long initialMillis = initialDelay.toMillis();
        long maximumMillis = maxDelay.toMillis();
        long delayMillis = initialMillis > maximumMillis / multiplier
                ? maximumMillis
                : initialMillis * multiplier;
        return Duration.ofMillis(delayMillis);
    }
}
