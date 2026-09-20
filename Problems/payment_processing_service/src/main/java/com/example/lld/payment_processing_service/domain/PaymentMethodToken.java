package com.example.lld.payment_processing_service.domain;

import java.util.Objects;

/** Opaque provider-safe token; raw payment credentials never enter this domain. */
public record PaymentMethodToken(String value) {
    public PaymentMethodToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Payment method token cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "PaymentMethodToken[redacted]";
    }
}
