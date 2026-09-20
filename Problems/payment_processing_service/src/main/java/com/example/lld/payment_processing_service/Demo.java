package com.example.lld.payment_processing_service;

import com.example.lld.payment_processing_service.domain.CallbackDisposition;
import com.example.lld.payment_processing_service.domain.CallbackResult;
import com.example.lld.payment_processing_service.domain.CommandResult;
import com.example.lld.payment_processing_service.domain.Money;
import com.example.lld.payment_processing_service.domain.PaymentMethodToken;
import com.example.lld.payment_processing_service.domain.PaymentOperation;
import com.example.lld.payment_processing_service.domain.PaymentSnapshot;
import com.example.lld.payment_processing_service.domain.PaymentStatus;
import com.example.lld.payment_processing_service.exception.AmountLimitExceededException;
import com.example.lld.payment_processing_service.provider.FakePaymentProvider;
import com.example.lld.payment_processing_service.provider.ProviderCallback;
import com.example.lld.payment_processing_service.provider.ProviderOutcome;
import com.example.lld.payment_processing_service.service.PaymentService;
import java.time.Clock;
import java.util.List;

/** Runnable assertions for the important success, failure, retry, and callback paths. */
public final class Demo {
    private static final PaymentMethodToken TOKEN = new PaymentMethodToken("tok_demo_opaque");

    private Demo() {}

    public static void main(String[] args) {
        FakePaymentProvider provider = new FakePaymentProvider("deterministic-pay");
        PaymentService service = new PaymentService(Clock.systemUTC(), List.of(provider));

        successfulPartialCaptureAndIdempotentRetry(service, provider);
        declinedAuthorization(service, provider);
        timeoutLateCallbackAndStaleCallback(service, provider);
        partialRefundAndDuplicateCallback(service, provider);
        voidAuthorization(service);

        System.out.println("All payment processing scenarios passed.");
    }

    private static void successfulPartialCaptureAndIdempotentRetry(
            PaymentService service, FakePaymentProvider provider) {
        create(service, "pay-success", "order-100", "100.00", "create-success");
        CommandResult authorized = service.authorize("pay-success", "authorize-success");
        check(authorized.payment().status() == PaymentStatus.AUTHORIZED, "authorization failed");

        service.capture("pay-success", usd("40.00"), "capture-first");
        int callsBefore = provider.callCount();
        CommandResult second = service.capture("pay-success", usd("60.00"), "capture-second");
        CommandResult replay = service.capture("pay-success", usd("60.00"), "capture-second");
        check(second == replay, "idempotent replay did not return the original result");
        check(provider.callCount() == callsBefore + 1, "capture replay called provider twice");
        check(second.payment().capturedTotal().equals(usd("100.00")), "capture total is wrong");
        check(second.payment().status() == PaymentStatus.CAPTURED, "payment is not captured");

        boolean excessiveRejected = false;
        try {
            service.capture("pay-success", usd("0.01"), "capture-excessive");
        } catch (AmountLimitExceededException expected) {
            excessiveRejected = true;
        }
        check(excessiveRejected, "excessive capture was accepted");
        System.out.println("success + two partial captures + idempotent retry: CAPTURED");
    }

    private static void declinedAuthorization(
            PaymentService service, FakePaymentProvider provider) {
        provider.enqueue(PaymentOperation.AUTHORIZE, ProviderOutcome.DECLINED, "do not honor");
        create(service, "pay-decline", "order-200", "25.00", "create-decline");
        PaymentSnapshot declined = service.authorize("pay-decline", "authorize-decline").payment();
        check(declined.status() == PaymentStatus.DECLINED, "decline was not terminal");
        check(declined.pendingProviderRequestId().isEmpty(), "decline left pending work");
        System.out.println("provider decline: DECLINED");
    }

    private static void timeoutLateCallbackAndStaleCallback(
            PaymentService service, FakePaymentProvider provider) {
        provider.enqueue(PaymentOperation.AUTHORIZE, ProviderOutcome.UNKNOWN, "network timeout");
        create(service, "pay-timeout", "order-300", "30.00", "create-timeout");
        CommandResult timedOut = service.authorize("pay-timeout", "authorize-timeout");
        check(timedOut.payment().status() == PaymentStatus.AUTHORIZING, "timeout regressed state");
        String requestId = timedOut.providerAttempt().orElseThrow().providerRequestId();

        ProviderCallback lateSuccess = provider.terminalCallback(
                requestId, "callback-auth-success", ProviderOutcome.APPROVED, "authorized later");
        CallbackResult applied = service.handleCallback(lateSuccess);
        check(applied.disposition() == CallbackDisposition.APPLIED, "late callback was not applied");
        check(applied.payment().status() == PaymentStatus.AUTHORIZED, "late auth did not succeed");

        ProviderCallback outOfOrder = provider.terminalCallback(
                requestId, "callback-auth-old", ProviderOutcome.DECLINED, "older decline arrived last");
        CallbackResult stale = service.handleCallback(outOfOrder);
        check(stale.disposition() == CallbackDisposition.STALE, "out-of-order callback not ignored");
        check(stale.payment().status() == PaymentStatus.AUTHORIZED, "stale callback regressed state");
        System.out.println("timeout + late callback + out-of-order callback: AUTHORIZED");
    }

    private static void partialRefundAndDuplicateCallback(
            PaymentService service, FakePaymentProvider provider) {
        provider.enqueue(PaymentOperation.REFUND, ProviderOutcome.UNKNOWN, "refund timed out");
        CommandResult timedOut = service.refund("pay-success", usd("25.00"), "refund-partial");
        String requestId = timedOut.providerAttempt().orElseThrow().providerRequestId();
        ProviderCallback callback = provider.terminalCallback(
                requestId, "callback-refund-25", ProviderOutcome.APPROVED, "refund completed");
        CallbackResult first = service.handleCallback(callback);
        CallbackResult duplicate = service.handleCallback(callback);
        check(first.disposition() == CallbackDisposition.APPLIED, "refund callback not applied");
        check(duplicate.disposition() == CallbackDisposition.DUPLICATE, "duplicate not ignored");
        check(first.payment().refundedTotal().equals(usd("25.00")), "refund total is wrong");
        check(first.payment().status() == PaymentStatus.PARTIALLY_REFUNDED, "refund state is wrong");
        check(duplicate.payment().refundedTotal().equals(usd("25.00")), "refund happened twice");
        System.out.println("partial refund + duplicate callback: PARTIALLY_REFUNDED once");
    }

    private static void voidAuthorization(PaymentService service) {
        create(service, "pay-void", "order-400", "15.00", "create-void");
        service.authorize("pay-void", "authorize-void");
        PaymentSnapshot voided = service.voidAuthorization("pay-void", "void-command").payment();
        check(voided.status() == PaymentStatus.VOIDED, "authorization was not voided");
        System.out.println("uncaptured authorization: VOIDED");
    }

    private static void create(
            PaymentService service,
            String paymentId,
            String orderId,
            String amount,
            String idempotencyKey) {
        CommandResult created = service.createPayment(
                paymentId, orderId, usd(amount), TOKEN, "deterministic-pay", idempotencyKey);
        check(created.payment().status() == PaymentStatus.CREATED, "payment was not created");
    }

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
