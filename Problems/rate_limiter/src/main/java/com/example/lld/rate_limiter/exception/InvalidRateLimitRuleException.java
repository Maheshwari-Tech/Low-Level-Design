package com.example.lld.rate_limiter.exception;

public final class InvalidRateLimitRuleException extends RateLimitException {
    private static final long serialVersionUID = 1L;

    public InvalidRateLimitRuleException(String message) {
        super(message);
    }
}
