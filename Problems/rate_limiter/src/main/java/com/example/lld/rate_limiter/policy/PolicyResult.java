package com.example.lld.rate_limiter.policy;

import java.time.Duration;
import java.util.Objects;

public record PolicyResult(
        boolean allowed,
        long remaining,
        long resetAtNanos,
        Duration resetAfter,
        Duration retryAfter) {
    public PolicyResult {
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining cannot be negative");
        }
        Objects.requireNonNull(resetAfter, "resetAfter");
        Objects.requireNonNull(retryAfter, "retryAfter");
    }
}
