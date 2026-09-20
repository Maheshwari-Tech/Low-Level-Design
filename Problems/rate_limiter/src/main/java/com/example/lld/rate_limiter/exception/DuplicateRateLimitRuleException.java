package com.example.lld.rate_limiter.exception;

public final class DuplicateRateLimitRuleException extends RateLimitException {
    private static final long serialVersionUID = 1L;

    public DuplicateRateLimitRuleException(String ruleId) {
        super("Rate-limit rule already exists: " + ruleId);
    }
}
