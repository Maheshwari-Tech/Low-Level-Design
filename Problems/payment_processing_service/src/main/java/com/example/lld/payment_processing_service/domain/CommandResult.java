package com.example.lld.payment_processing_service.domain;

import java.util.Objects;
import java.util.Optional;

/** A frozen command result. Idempotent replays return this original value. */
public record CommandResult(
        String idempotencyKey,
        PaymentSnapshot payment,
        Optional<PaymentAttempt> providerAttempt) {
    public CommandResult {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(payment, "payment");
        providerAttempt = Objects.requireNonNull(providerAttempt, "providerAttempt");
    }
}
