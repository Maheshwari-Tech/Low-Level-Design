package com.example.lld.rate_limiter.exception;

public final class UnknownRateLimitRuleException extends RateLimitException {
    private static final long serialVersionUID = 1L;

    public UnknownRateLimitRuleException(String ruleId) {
        super("Unknown rate-limit rule: " + ruleId);
    }
}
