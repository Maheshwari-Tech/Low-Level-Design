package com.example.lld.rate_limiter.exception;

public final class InvalidRateLimitRequestException extends RateLimitException {
    private static final long serialVersionUID = 1L;

    public InvalidRateLimitRequestException(String message) {
        super(message);
    }
}
