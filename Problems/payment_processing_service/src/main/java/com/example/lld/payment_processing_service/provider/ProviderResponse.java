package com.example.lld.payment_processing_service.provider;

import java.util.Objects;
import java.util.Optional;

public record ProviderResponse(
        ProviderOutcome outcome, Optional<String> providerReference, String detail) {
    public ProviderResponse {
        Objects.requireNonNull(outcome, "outcome");
        providerReference = Objects.requireNonNull(providerReference, "providerReference");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public static ProviderResponse transientFailure(String detail) {
        return new ProviderResponse(ProviderOutcome.TRANSIENT_FAILURE, Optional.empty(), detail);
    }
}
