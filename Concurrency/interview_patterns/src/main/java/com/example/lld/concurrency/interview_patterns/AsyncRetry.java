package com.example.lld.concurrency.interview_patterns;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Composes asynchronous attempts without sleeping or blocking a worker thread.
 * The operation must reuse the same provider idempotency key on every attempt.
 */
public final class AsyncRetry {
    private AsyncRetry() {}

    public static <T> CompletionStage<T> execute(
            Supplier<? extends CompletionStage<T>> operation,
            RetryPolicy policy,
            ScheduledExecutorService scheduler) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(scheduler, "scheduler");
        CompletableFuture<T> result = new CompletableFuture<>();
        attempt(operation, policy, scheduler, 1, result);
        return result;
    }

    private static <T> void attempt(
            Supplier<? extends CompletionStage<T>> operation,
            RetryPolicy policy,
            ScheduledExecutorService scheduler,
            int attemptNumber,
            CompletableFuture<T> result) {
        if (result.isDone()) {
            return;
        }

        CompletionStage<T> attempt;
        try {
            attempt = Objects.requireNonNull(operation.get(), "operation returned null stage");
        } catch (Throwable failure) {
            attempt = CompletableFuture.failedFuture(failure);
        }

        attempt.whenComplete((value, failure) -> {
            if (failure == null) {
                result.complete(value);
                return;
            }

            Throwable cause = unwrap(failure);
            if (attemptNumber >= policy.maxAttempts() || !policy.retryable().test(cause)) {
                result.completeExceptionally(cause);
                return;
            }

            int nextAttempt = attemptNumber + 1;
            Duration delay = policy.delayBeforeAttempt(nextAttempt);
            scheduler.schedule(
                    () -> attempt(operation, policy, scheduler, nextAttempt, result),
                    delay.toNanos(),
                    TimeUnit.NANOSECONDS);
        });
    }

    private static Throwable unwrap(Throwable failure) {
        if ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null) {
            return failure.getCause();
        }
        return failure;
    }
}
