package com.example.lld.payment_processing_service.domain;

import java.util.Objects;

public record CallbackResult(CallbackDisposition disposition, PaymentSnapshot payment) {
    public CallbackResult {
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(payment, "payment");
    }
}
