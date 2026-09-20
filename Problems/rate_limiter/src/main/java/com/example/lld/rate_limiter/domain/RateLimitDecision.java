package com.example.lld.rate_limiter.domain;

import java.time.Duration;
import java.util.Objects;

/** Metadata is calculated under the same per-key lock as the allow/deny mutation. */
public record RateLimitDecision(
        boolean allowed,
        String ruleId,
        RateLimitKey key,
        PolicyType policyType,
        long limit,
        long remaining,
        long resetAtNanos,
        Duration resetAfter,
        Duration retryAfter,
        long ruleVersion) {
    public RateLimitDecision {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(policyType, "policyType");
        if (limit <= 0 || remaining < 0 || remaining > limit) {
            throw new IllegalArgumentException("Invalid limit metadata");
        }
        Objects.requireNonNull(resetAfter, "resetAfter");
        Objects.requireNonNull(retryAfter, "retryAfter");
        if (resetAfter.isNegative() || retryAfter.isNegative()) {
            throw new IllegalArgumentException("Durations cannot be negative");
        }
        if (ruleVersion <= 0) {
            throw new IllegalArgumentException("ruleVersion must be positive");
        }
    }
}
