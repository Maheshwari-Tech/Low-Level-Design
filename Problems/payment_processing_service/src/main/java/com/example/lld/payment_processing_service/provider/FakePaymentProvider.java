package com.example.lld.payment_processing_service.provider;

import com.example.lld.payment_processing_service.domain.PaymentOperation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic provider adapter for examples and tests. Plans are consumed FIFO per
 * operation; an unplanned request succeeds.
 */
public final class FakePaymentProvider implements PaymentProvider {
    private record Plan(ProviderOutcome outcome, String detail) {}

    private record RecordedCall(ProviderRequest request, ProviderResponse response) {}

    private final String name;
    private final Map<PaymentOperation, Deque<Plan>> plans =
            new EnumMap<>(PaymentOperation.class);
    private final Map<String, RecordedCall> calls = new LinkedHashMap<>();
    private long providerReferenceSequence;

    public FakePaymentProvider(String name) {
        this.name = requireText(name, "name");
        for (PaymentOperation operation : PaymentOperation.values()) {
            plans.put(operation, new ArrayDeque<>());
        }
    }

    @Override
    public String name() {
        return name;
    }

    public synchronized void enqueue(
            PaymentOperation operation, ProviderOutcome outcome, String detail) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        plans.get(operation).addLast(new Plan(outcome, Objects.requireNonNull(detail, "detail")));
    }

    @Override
    public ProviderResponse authorize(ProviderRequest request) {
        return execute(PaymentOperation.AUTHORIZE, request);
    }

    @Override
    public ProviderResponse capture(ProviderRequest request) {
        return execute(PaymentOperation.CAPTURE, request);
    }

    @Override
    public ProviderResponse voidAuthorization(ProviderRequest request) {
        return execute(PaymentOperation.VOID, request);
    }

    @Override
    public ProviderResponse refund(ProviderRequest request) {
        return execute(PaymentOperation.REFUND, request);
    }

    public synchronized ProviderCallback terminalCallback(
            String providerRequestId,
            String callbackId,
            ProviderOutcome terminalOutcome,
            String detail) {
        if (terminalOutcome == ProviderOutcome.UNKNOWN) {
            throw new IllegalArgumentException("Callback outcome must be terminal");
        }
        RecordedCall call = calls.get(providerRequestId);
        if (call == null) {
            throw new IllegalArgumentException("Unknown provider request: " + providerRequestId);
        }
        ProviderRequest request = call.request();
        String providerReference = call.response().providerReference()
                .orElseGet(() -> nextProviderReference("callback"));
        return new ProviderCallback(
                requireText(callbackId, "callbackId"),
                name,
                providerRequestId,
                request.paymentId(),
                request.operation(),
                request.amount(),
                terminalOutcome,
                Optional.of(providerReference),
                Objects.requireNonNull(detail, "detail"));
    }

    public synchronized int callCount() {
        return calls.size();
    }

    private synchronized ProviderResponse execute(
            PaymentOperation expectedOperation, ProviderRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.operation() != expectedOperation) {
            throw new IllegalArgumentException(
                    "Expected " + expectedOperation + " but got " + request.operation());
        }
        RecordedCall replay = calls.get(request.providerRequestId());
        if (replay != null) {
            if (!replay.request().equals(request)) {
                throw new IllegalArgumentException(
                        "Provider request id was reused with different parameters");
            }
            return replay.response();
        }

        Plan plan = plans.get(expectedOperation).pollFirst();
        if (plan == null) {
            plan = new Plan(ProviderOutcome.APPROVED, "approved by default plan");
        }
        ProviderResponse response = new ProviderResponse(
                plan.outcome(), Optional.of(nextProviderReference("request")), plan.detail());
        calls.put(request.providerRequestId(), new RecordedCall(request, response));
        return response;
    }

    private String nextProviderReference(String source) {
        providerReferenceSequence++;
        return name + "-" + source + "-" + providerReferenceSequence;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
