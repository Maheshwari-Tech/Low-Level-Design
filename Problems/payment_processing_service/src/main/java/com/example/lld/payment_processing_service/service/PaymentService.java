package com.example.lld.payment_processing_service.service;

import com.example.lld.payment_processing_service.domain.CallbackDisposition;
import com.example.lld.payment_processing_service.domain.CallbackResult;
import com.example.lld.payment_processing_service.domain.CommandResult;
import com.example.lld.payment_processing_service.domain.Money;
import com.example.lld.payment_processing_service.domain.Payment;
import com.example.lld.payment_processing_service.domain.PaymentAttempt;
import com.example.lld.payment_processing_service.domain.PaymentMethodToken;
import com.example.lld.payment_processing_service.domain.PaymentOperation;
import com.example.lld.payment_processing_service.domain.PaymentSnapshot;
import com.example.lld.payment_processing_service.exception.DuplicatePaymentException;
import com.example.lld.payment_processing_service.exception.IdempotencyConflictException;
import com.example.lld.payment_processing_service.exception.PaymentDomainException;
import com.example.lld.payment_processing_service.exception.PaymentNotFoundException;
import com.example.lld.payment_processing_service.exception.ProviderCallbackException;
import com.example.lld.payment_processing_service.provider.PaymentProvider;
import com.example.lld.payment_processing_service.provider.ProviderCallback;
import com.example.lld.payment_processing_service.provider.ProviderRequest;
import com.example.lld.payment_processing_service.provider.ProviderResponse;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Application service coordinating aggregates, idempotency, providers, and callbacks. */
public final class PaymentService {
    private record CommandFingerprint(String command, String paymentId, String arguments) {}

    private static final class IdempotencyEntry {
        private final CommandFingerprint fingerprint;
        private final CompletableFuture<CommandResult> result = new CompletableFuture<>();

        private IdempotencyEntry(CommandFingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }
    }

    private final Clock clock;
    private final Map<String, PaymentProvider> providers;
    private final ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IdempotencyEntry> idempotency =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> callbackRoutes = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public PaymentService(Clock clock, Collection<? extends PaymentProvider> providers) {
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(providers, "providers");
        Map<String, PaymentProvider> byName = new LinkedHashMap<>();
        for (PaymentProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            PaymentProvider duplicate = byName.putIfAbsent(provider.name(), provider);
            if (duplicate != null) {
                throw new IllegalArgumentException("Duplicate provider name: " + provider.name());
            }
        }
        if (byName.isEmpty()) {
            throw new IllegalArgumentException("At least one provider is required");
        }
        this.providers = Map.copyOf(byName);
    }

    public CommandResult createPayment(
            String paymentId,
            String orderId,
            Money amount,
            PaymentMethodToken paymentMethodToken,
            String providerName,
            String idempotencyKey) {
        requireProvider(providerName);
        CommandFingerprint fingerprint = new CommandFingerprint(
                "CREATE",
                paymentId,
                orderId + "|" + amount + "|" + paymentMethodToken.value() + "|" + providerName);
        return idempotently(idempotencyKey, fingerprint, () -> {
            Payment payment = new Payment(
                    paymentId,
                    orderId,
                    amount,
                    paymentMethodToken,
                    providerName,
                    clock);
            if (payments.putIfAbsent(paymentId, payment) != null) {
                throw new DuplicatePaymentException(paymentId);
            }
            return new CommandResult(idempotencyKey, payment.snapshot(), Optional.empty());
        });
    }

    public CommandResult authorize(String paymentId, String idempotencyKey) {
        Payment payment = requirePayment(paymentId);
        return providerCommand(
                payment,
                PaymentOperation.AUTHORIZE,
                payment.amount(),
                idempotencyKey);
    }

    public CommandResult capture(String paymentId, Money amount, String idempotencyKey) {
        return providerCommand(
                requirePayment(paymentId), PaymentOperation.CAPTURE, amount, idempotencyKey);
    }

    public CommandResult voidAuthorization(String paymentId, String idempotencyKey) {
        Payment payment = requirePayment(paymentId);
        return providerCommand(payment, PaymentOperation.VOID, payment.amount(), idempotencyKey);
    }

    public CommandResult refund(String paymentId, Money amount, String idempotencyKey) {
        return providerCommand(
                requirePayment(paymentId), PaymentOperation.REFUND, amount, idempotencyKey);
    }

    public CallbackResult handleCallback(ProviderCallback callback) {
        Objects.requireNonNull(callback, "callback");
        String routedPaymentId = callbackRoutes.get(callback.providerRequestId());
        if (routedPaymentId == null) {
            throw new ProviderCallbackException(
                    "No payment route for provider request " + callback.providerRequestId());
        }
        if (!routedPaymentId.equals(callback.paymentId())) {
            throw new ProviderCallbackException("Callback payment does not match stored route");
        }
        Payment payment = requirePayment(routedPaymentId);
        CallbackDisposition disposition = payment.reconcile(callback);
        return new CallbackResult(disposition, payment.snapshot());
    }

    public PaymentSnapshot getPayment(String paymentId) {
        return requirePayment(paymentId).snapshot();
    }

    private CommandResult providerCommand(
            Payment payment,
            PaymentOperation operation,
            Money operationAmount,
            String idempotencyKey) {
        Objects.requireNonNull(operationAmount, "operationAmount");
        CommandFingerprint fingerprint = new CommandFingerprint(
                operation.name(), payment.paymentId(), operationAmount.toString());
        return idempotently(idempotencyKey, fingerprint, () -> {
            long next = sequence.incrementAndGet();
            String attemptId = "attempt-" + next;
            String providerRequestId = "provider-request-" + next;
            payment.beginAttempt(attemptId, providerRequestId, operation, operationAmount);
            String previousRoute = callbackRoutes.putIfAbsent(
                    providerRequestId, payment.paymentId());
            if (previousRoute != null) {
                throw new IllegalStateException("Generated duplicate provider request id");
            }

            ProviderRequest request = new ProviderRequest(
                    providerRequestId,
                    payment.paymentId(),
                    payment.orderId(),
                    operation,
                    operationAmount,
                    payment.paymentMethodToken());
            ProviderResponse response;
            try {
                response = invokeProvider(requireProvider(payment.providerName()), operation, request);
            } catch (RuntimeException providerError) {
                response = ProviderResponse.transientFailure(
                        "provider adapter threw " + providerError.getClass().getSimpleName());
            }
            PaymentAttempt completed = payment.completeAttempt(providerRequestId, response);
            return new CommandResult(
                    idempotencyKey, payment.snapshot(), Optional.of(completed));
        });
    }

    private static ProviderResponse invokeProvider(
            PaymentProvider provider, PaymentOperation operation, ProviderRequest request) {
        return switch (operation) {
            case AUTHORIZE -> provider.authorize(request);
            case CAPTURE -> provider.capture(request);
            case VOID -> provider.voidAuthorization(request);
            case REFUND -> provider.refund(request);
        };
    }

    private CommandResult idempotently(
            String idempotencyKey,
            CommandFingerprint fingerprint,
            Supplier<CommandResult> command) {
        requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(command, "command");
        IdempotencyEntry candidate = new IdempotencyEntry(fingerprint);
        IdempotencyEntry entry = idempotency.putIfAbsent(idempotencyKey, candidate);
        if (entry == null) {
            try {
                CommandResult result = command.get();
                candidate.result.complete(result);
                return result;
            } catch (RuntimeException | Error error) {
                candidate.result.completeExceptionally(error);
                throw error;
            }
        }
        if (!entry.fingerprint.equals(fingerprint)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        try {
            return entry.result.join();
        } catch (CompletionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error fatal) {
                throw fatal;
            }
            throw new PaymentDomainException("Idempotent command failed", cause);
        }
    }

    private Payment requirePayment(String paymentId) {
        Payment payment = payments.get(requireText(paymentId, "paymentId"));
        if (payment == null) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    private PaymentProvider requireProvider(String providerName) {
        PaymentProvider provider = providers.get(requireText(providerName, "providerName"));
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        return provider;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
