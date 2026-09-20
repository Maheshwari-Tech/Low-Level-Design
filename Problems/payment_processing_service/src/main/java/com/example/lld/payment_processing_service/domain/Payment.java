package com.example.lld.payment_processing_service.domain;

import com.example.lld.payment_processing_service.exception.AmountLimitExceededException;
import com.example.lld.payment_processing_service.exception.InvalidMoneyException;
import com.example.lld.payment_processing_service.exception.InvalidPaymentStateException;
import com.example.lld.payment_processing_service.exception.OperationInProgressException;
import com.example.lld.payment_processing_service.exception.ProviderCallbackException;
import com.example.lld.payment_processing_service.provider.ProviderCallback;
import com.example.lld.payment_processing_service.provider.ProviderOutcome;
import com.example.lld.payment_processing_service.provider.ProviderResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Thread-safe payment aggregate. All invariants are enforced inside its monitor. */
public final class Payment {
    private final String paymentId;
    private final String orderId;
    private final Money amount;
    private final PaymentMethodToken paymentMethodToken;
    private final String providerName;
    private final Clock clock;
    private final Map<String, PaymentAttempt> attemptsByRequestId = new LinkedHashMap<>();
    private final List<AuditEntry> auditHistory = new ArrayList<>();
    private final Set<String> callbackIds = new LinkedHashSet<>();

    private Money capturedTotal;
    private Money refundedTotal;
    private PaymentStatus status = PaymentStatus.CREATED;
    private boolean authorized;
    private boolean voided;
    private String pendingProviderRequestId;
    private long auditSequence;

    public Payment(
            String paymentId,
            String orderId,
            Money amount,
            PaymentMethodToken paymentMethodToken,
            String providerName,
            Clock clock) {
        this.paymentId = requireText(paymentId, "paymentId");
        this.orderId = requireText(orderId, "orderId");
        this.amount = Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new InvalidMoneyException("Payment amount must be positive");
        }
        this.paymentMethodToken = Objects.requireNonNull(paymentMethodToken, "paymentMethodToken");
        this.providerName = requireText(providerName, "providerName");
        this.clock = Objects.requireNonNull(clock, "clock");
        capturedTotal = Money.zero(amount.currency());
        refundedTotal = Money.zero(amount.currency());
        audit("PAYMENT_CREATED", "order=" + orderId + ", amount=" + amount);
    }

    public String paymentId() {
        return paymentId;
    }

    public String orderId() {
        return orderId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMethodToken paymentMethodToken() {
        return paymentMethodToken;
    }

    public String providerName() {
        return providerName;
    }

    public synchronized PaymentAttempt beginAttempt(
            String attemptId,
            String providerRequestId,
            PaymentOperation operation,
            Money operationAmount) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(operationAmount, "operationAmount");
        amount.requireSameCurrency(operationAmount);
        if (pendingProviderRequestId != null) {
            throw new OperationInProgressException(paymentId, pendingProviderRequestId);
        }
        validateOperation(operation, operationAmount);
        if (attemptsByRequestId.containsKey(providerRequestId)) {
            throw new IllegalArgumentException("Duplicate provider request id: " + providerRequestId);
        }

        Instant now = clock.instant();
        PaymentAttempt attempt = new PaymentAttempt(
                requireText(attemptId, "attemptId"),
                requireText(providerRequestId, "providerRequestId"),
                operation,
                operationAmount,
                AttemptStatus.PENDING,
                Optional.empty(),
                "provider request started",
                now,
                now);
        attemptsByRequestId.put(providerRequestId, attempt);
        pendingProviderRequestId = providerRequestId;
        if (operation == PaymentOperation.AUTHORIZE) {
            status = PaymentStatus.AUTHORIZING;
        }
        audit("ATTEMPT_STARTED", describe(attempt));
        return attempt;
    }

    public synchronized PaymentAttempt completeAttempt(
            String providerRequestId, ProviderResponse response) {
        Objects.requireNonNull(response, "response");
        return settleAttempt(
                providerRequestId,
                response.outcome(),
                response.providerReference(),
                response.detail(),
                "SYNC_RESPONSE");
    }

    public synchronized CallbackDisposition reconcile(ProviderCallback callback) {
        Objects.requireNonNull(callback, "callback");
        if (callbackIds.contains(callback.callbackId())) {
            audit("DUPLICATE_CALLBACK_IGNORED", "callback=" + callback.callbackId());
            return CallbackDisposition.DUPLICATE;
        }
        callbackIds.add(callback.callbackId());
        if (!paymentId.equals(callback.paymentId())) {
            throw new ProviderCallbackException("Callback payment id does not match its route");
        }
        if (!providerName.equals(callback.providerName())) {
            throw new ProviderCallbackException("Callback provider does not match the payment");
        }

        PaymentAttempt attempt = attemptsByRequestId.get(callback.providerRequestId());
        if (attempt == null) {
            throw new ProviderCallbackException(
                    "Callback references an unknown attempt: " + callback.providerRequestId());
        }
        if (attempt.operation() != callback.operation() || !attempt.amount().equals(callback.amount())) {
            throw new ProviderCallbackException("Callback operation or amount does not match attempt");
        }
        if (attempt.status() != AttemptStatus.PENDING
                && attempt.status() != AttemptStatus.UNKNOWN) {
            audit(
                    "STALE_CALLBACK_IGNORED",
                    "callback=" + callback.callbackId() + ", attempt=" + attempt.attemptId());
            return CallbackDisposition.STALE;
        }

        settleAttempt(
                callback.providerRequestId(),
                callback.outcome(),
                callback.providerReference(),
                callback.detail(),
                "CALLBACK");
        return CallbackDisposition.APPLIED;
    }

    public synchronized PaymentAttempt attempt(String providerRequestId) {
        PaymentAttempt attempt = attemptsByRequestId.get(providerRequestId);
        if (attempt == null) {
            throw new IllegalArgumentException("Unknown attempt: " + providerRequestId);
        }
        return attempt;
    }

    public synchronized PaymentSnapshot snapshot() {
        return new PaymentSnapshot(
                paymentId,
                orderId,
                providerName,
                amount,
                capturedTotal,
                refundedTotal,
                status,
                Optional.ofNullable(pendingProviderRequestId),
                List.copyOf(attemptsByRequestId.values()),
                List.copyOf(auditHistory));
    }

    private PaymentAttempt settleAttempt(
            String providerRequestId,
            ProviderOutcome outcome,
            Optional<String> providerReference,
            String detail,
            String source) {
        PaymentAttempt current = attemptsByRequestId.get(providerRequestId);
        if (current == null) {
            throw new ProviderCallbackException("Unknown provider request: " + providerRequestId);
        }
        if (current.status() != AttemptStatus.PENDING
                && current.status() != AttemptStatus.UNKNOWN) {
            audit(source + "_IGNORED", "attempt already terminal: " + current.attemptId());
            return current;
        }
        if (outcome == ProviderOutcome.UNKNOWN) {
            PaymentAttempt unknown = updatedAttempt(
                    current, AttemptStatus.UNKNOWN, providerReference, detail);
            attemptsByRequestId.put(providerRequestId, unknown);
            audit("OUTCOME_UNKNOWN", source + ": " + describe(unknown));
            return unknown;
        }

        AttemptStatus terminalStatus = switch (outcome) {
            case APPROVED -> AttemptStatus.SUCCEEDED;
            case DECLINED -> AttemptStatus.DECLINED;
            case TRANSIENT_FAILURE -> AttemptStatus.FAILED;
            case UNKNOWN -> throw new IllegalStateException("UNKNOWN handled above");
        };
        PaymentAttempt settled =
                updatedAttempt(current, terminalStatus, providerReference, detail);
        attemptsByRequestId.put(providerRequestId, settled);
        pendingProviderRequestId = null;
        if (outcome == ProviderOutcome.APPROVED) {
            applySuccessfulOperation(current.operation(), current.amount());
        } else {
            applyFailedOperation(current.operation(), outcome);
        }
        audit("ATTEMPT_SETTLED", source + ": " + describe(settled) + ", payment=" + status);
        return settled;
    }

    private PaymentAttempt updatedAttempt(
            PaymentAttempt current,
            AttemptStatus attemptStatus,
            Optional<String> providerReference,
            String detail) {
        return new PaymentAttempt(
                current.attemptId(),
                current.providerRequestId(),
                current.operation(),
                current.amount(),
                attemptStatus,
                Objects.requireNonNull(providerReference, "providerReference"),
                Objects.requireNonNull(detail, "detail"),
                current.startedAt(),
                clock.instant());
    }

    private void validateOperation(PaymentOperation operation, Money operationAmount) {
        if (!operationAmount.isPositive()) {
            throw new InvalidMoneyException(operation + " amount must be positive");
        }
        switch (operation) {
            case AUTHORIZE -> {
                if (status != PaymentStatus.CREATED && status != PaymentStatus.FAILED) {
                    throw invalidState(operation);
                }
                if (!operationAmount.equals(amount)) {
                    throw new InvalidMoneyException("Authorization must equal the payment amount");
                }
            }
            case CAPTURE -> {
                requireActiveAuthorization(operation);
                Money available = amount.subtract(capturedTotal);
                if (operationAmount.compareTo(available) > 0) {
                    throw new AmountLimitExceededException(
                            "Capture " + operationAmount + " exceeds available " + available);
                }
            }
            case VOID -> {
                requireActiveAuthorization(operation);
                if (!capturedTotal.isZero()) {
                    throw new InvalidPaymentStateException(
                            "An authorization cannot be voided after capture");
                }
                if (!operationAmount.equals(amount)) {
                    throw new InvalidMoneyException("Void amount must equal the authorization");
                }
            }
            case REFUND -> {
                requireActiveAuthorization(operation);
                Money refundable = capturedTotal.subtract(refundedTotal);
                if (operationAmount.compareTo(refundable) > 0) {
                    throw new AmountLimitExceededException(
                            "Refund " + operationAmount + " exceeds refundable " + refundable);
                }
            }
        }
    }

    private void requireActiveAuthorization(PaymentOperation operation) {
        if (!authorized || voided) {
            throw invalidState(operation);
        }
    }

    private InvalidPaymentStateException invalidState(PaymentOperation operation) {
        return new InvalidPaymentStateException(
                operation + " is not allowed while payment " + paymentId + " is " + status);
    }

    private void applySuccessfulOperation(PaymentOperation operation, Money operationAmount) {
        switch (operation) {
            case AUTHORIZE -> authorized = true;
            case CAPTURE -> capturedTotal = capturedTotal.add(operationAmount);
            case VOID -> voided = true;
            case REFUND -> refundedTotal = refundedTotal.add(operationAmount);
        }
        recalculateStatus();
    }

    private void applyFailedOperation(PaymentOperation operation, ProviderOutcome outcome) {
        if (operation == PaymentOperation.AUTHORIZE) {
            status = outcome == ProviderOutcome.DECLINED
                    ? PaymentStatus.DECLINED
                    : PaymentStatus.FAILED;
        } else {
            recalculateStatus();
        }
    }

    private void recalculateStatus() {
        if (voided) {
            status = PaymentStatus.VOIDED;
        } else if (!authorized) {
            status = PaymentStatus.CREATED;
        } else if (!refundedTotal.isZero()) {
            status = refundedTotal.equals(capturedTotal)
                    ? PaymentStatus.REFUNDED
                    : PaymentStatus.PARTIALLY_REFUNDED;
        } else if (capturedTotal.isZero()) {
            status = PaymentStatus.AUTHORIZED;
        } else if (capturedTotal.equals(amount)) {
            status = PaymentStatus.CAPTURED;
        } else {
            status = PaymentStatus.PARTIALLY_CAPTURED;
        }
    }

    private void audit(String event, String detail) {
        auditSequence++;
        auditHistory.add(new AuditEntry(auditSequence, clock.instant(), event, detail));
    }

    private static String describe(PaymentAttempt attempt) {
        return "attempt=" + attempt.attemptId() + ", operation=" + attempt.operation()
                + ", amount=" + attempt.amount() + ", outcome=" + attempt.status();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
