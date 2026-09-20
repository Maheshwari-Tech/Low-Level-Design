package com.example.lld.rate_limiter.domain;

import com.example.lld.rate_limiter.exception.InvalidRateLimitRequestException;
import java.util.Objects;

public record RateLimitRequest(String ruleId, RateLimitKey key, long cost) {
    public RateLimitRequest {
        Objects.requireNonNull(ruleId, "ruleId");
        if (ruleId.isBlank()) {
            throw new InvalidRateLimitRequestException("ruleId cannot be blank");
        }
        Objects.requireNonNull(key, "key");
        if (cost <= 0) {
            throw new InvalidRateLimitRequestException("cost must be positive");
        }
    }

    public static RateLimitRequest one(String ruleId, RateLimitKey key) {
        return new RateLimitRequest(ruleId, key, 1);
    }
}
