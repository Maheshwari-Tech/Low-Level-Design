package com.example.lld.payment_processing_service.provider;

/** Provider port. Each adapter must make providerRequestId idempotent upstream. */
public interface PaymentProvider {
    String name();

    ProviderResponse authorize(ProviderRequest request);

    ProviderResponse capture(ProviderRequest request);

    ProviderResponse voidAuthorization(ProviderRequest request);

    ProviderResponse refund(ProviderRequest request);
}
