package com.example.lld.notification_framework;

import java.time.Duration;
import java.util.Objects;

public record RetryPolicy(
        int maxAttemptsPerProvider,
        Duration initialBackoff,
        Duration maximumBackoff) {

    public RetryPolicy {
        if (maxAttemptsPerProvider <= 0) {
            throw new IllegalArgumentException("Maximum attempts must be positive");
        }
        Objects.requireNonNull(initialBackoff, "initialBackoff");
        Objects.requireNonNull(maximumBackoff, "maximumBackoff");
        if (initialBackoff.isNegative() || initialBackoff.isZero()) {
            throw new IllegalArgumentException("Initial backoff must be positive");
        }
        if (maximumBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("Maximum backoff cannot be less than initial backoff");
        }
    }

    /** Exponential backoff capped at maximumBackoff; the first failure uses initialBackoff. */
    public Duration delayAfterFailure(int failedAttemptNumber) {
        if (failedAttemptNumber <= 0) {
            throw new IllegalArgumentException("Failed attempt number must be positive");
        }
        Duration delay = initialBackoff;
        for (int attempt = 1; attempt < failedAttemptNumber; attempt++) {
            if (delay.compareTo(maximumBackoff.dividedBy(2L)) > 0) {
                return maximumBackoff;
            }
            delay = delay.multipliedBy(2L);
        }
        return delay.compareTo(maximumBackoff) > 0 ? maximumBackoff : delay;
    }
}
