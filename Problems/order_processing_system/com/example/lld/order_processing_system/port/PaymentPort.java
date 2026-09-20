package com.example.lld.order_processing_system.port;

import com.example.lld.order_processing_system.model.Money;

/** Boundary to a payment provider; charge/refund calls are idempotent by key. */
public interface PaymentPort {
    PaymentReceipt charge(String orderId, Money amount, String idempotencyKey);

    void refund(String paymentId, Money amount, String idempotencyKey);

    record PaymentReceipt(String paymentId) {
        public PaymentReceipt {
            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalArgumentException("paymentId must not be blank");
            }
        }
    }
}
