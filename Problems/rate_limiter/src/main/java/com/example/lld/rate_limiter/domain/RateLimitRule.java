package com.example.lld.rate_limiter.domain;

import com.example.lld.rate_limiter.exception.InvalidRateLimitRuleException;
import java.time.Duration;
import java.util.Objects;

/**
 * capacity tokens are replenished per period; burstAllowance enlarges maximum stored
 * capacity. For fixed windows, capacity + burstAllowance is the per-window limit.
 */
public record RateLimitRule(
        String ruleId,
        long capacity,
        Duration period,
        long burstAllowance,
        PolicyType policyType) {
    public RateLimitRule {
        Objects.requireNonNull(ruleId, "ruleId");
        if (ruleId.isBlank()) {
            throw new InvalidRateLimitRuleException("ruleId cannot be blank");
        }
        if (capacity <= 0) {
            throw new InvalidRateLimitRuleException("capacity must be positive");
        }
        Objects.requireNonNull(period, "period");
        if (period.isZero() || period.isNegative()) {
            throw new InvalidRateLimitRuleException("period must be positive");
        }
        try {
            if (period.toNanos() <= 0) {
                throw new InvalidRateLimitRuleException("period is too small");
            }
        } catch (ArithmeticException error) {
            throw new InvalidRateLimitRuleException("period is too large for nanosecond timing");
        }
        if (burstAllowance < 0) {
            throw new InvalidRateLimitRuleException("burstAllowance cannot be negative");
        }
        try {
            Math.addExact(capacity, burstAllowance);
        } catch (ArithmeticException error) {
            throw new InvalidRateLimitRuleException("capacity plus burstAllowance overflows");
        }
        Objects.requireNonNull(policyType, "policyType");
    }

    public long limit() {
        return capacity + burstAllowance;
    }

    public long periodNanos() {
        return period.toNanos();
    }
}
