package com.example.lld.notification_framework;

import java.util.Objects;

public final class ProviderFailureException extends Exception {
    private static final long serialVersionUID = 1L;

    private final FailureType failureType;

    public ProviderFailureException(FailureType failureType, String message) {
        super(message);
        this.failureType = Objects.requireNonNull(failureType, "failureType");
    }

    public FailureType failureType() {
        return failureType;
    }
}
