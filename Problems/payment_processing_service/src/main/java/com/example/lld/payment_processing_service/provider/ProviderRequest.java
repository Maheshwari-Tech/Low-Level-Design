package com.example.lld.payment_processing_service.provider;

import com.example.lld.payment_processing_service.domain.Money;
import com.example.lld.payment_processing_service.domain.PaymentMethodToken;
import com.example.lld.payment_processing_service.domain.PaymentOperation;
import java.util.Objects;

public record ProviderRequest(
        String providerRequestId,
        String paymentId,
        String orderId,
        PaymentOperation operation,
        Money amount,
        PaymentMethodToken paymentMethodToken) {
    public ProviderRequest {
        Objects.requireNonNull(providerRequestId, "providerRequestId");
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(paymentMethodToken, "paymentMethodToken");
    }
}
