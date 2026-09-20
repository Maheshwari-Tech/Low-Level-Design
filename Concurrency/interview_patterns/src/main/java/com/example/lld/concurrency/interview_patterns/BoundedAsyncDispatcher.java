package com.example.lld.concurrency.interview_patterns;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Bounded producer-consumer dispatcher.
 *
 * A single pump owns queue removal. A semaphore caps asynchronous in-flight work; completion
 * releases capacity, so no worker thread waits for remote I/O. submit() fails fast when the queue
 * is full, making backpressure explicit.
 */
public final class BoundedAsyncDispatcher<T, R> implements AutoCloseable {
    private record Envelope<T, R>(T message, CompletableFuture<R> result) {}

    private final ArrayBlockingQueue<Envelope<T, R>> queue;
    private final Semaphore inFlight;
    private final int maxInFlight;
    private final Function<? super T, ? extends CompletionStage<R>> handler;
    private final ExecutorService invocationExecutor;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final Thread pump;

    public BoundedAsyncDispatcher(
            int queueCapacity,
            int maxInFlight,
            Function<? super T, ? extends CompletionStage<R>> handler) {
        if (queueCapacity <= 0 || maxInFlight <= 0) {
            throw new IllegalArgumentException("capacities must be positive");
        }
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.maxInFlight = maxInFlight;
        this.inFlight = new Semaphore(maxInFlight);
        this.handler = Objects.requireNonNull(handler, "handler");
        this.invocationExecutor = Executors.newFixedThreadPool(maxInFlight);
        this.pump = new Thread(this::pumpLoop, "bounded-async-dispatcher");
        this.pump.start();
    }

    public CompletionStage<R> submit(T message) {
        if (!accepting.get()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("dispatcher closed"));
        }
        CompletableFuture<R> result = new CompletableFuture<>();
        Envelope<T, R> envelope = new Envelope<>(Objects.requireNonNull(message, "message"), result);
        if (!queue.offer(envelope)) {
            result.completeExceptionally(new RejectedExecutionException("dispatcher queue full"));
        }
        return result;
    }

    private void pumpLoop() {
        while (accepting.get() || !queue.isEmpty()) {
            try {
                Envelope<T, R> envelope = queue.poll(100, TimeUnit.MILLISECONDS);
                if (envelope == null) {
                    continue;
                }
                inFlight.acquire();
                invocationExecutor.execute(() -> invoke(envelope));
            } catch (InterruptedException interrupted) {
                if (!accepting.get() && queue.isEmpty()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void invoke(Envelope<T, R> envelope) {
        CompletionStage<R> stage;
        try {
            stage = Objects.requireNonNull(handler.apply(envelope.message()),
                    "handler returned null stage");
        } catch (Throwable failure) {
            stage = CompletableFuture.failedFuture(failure);
        }
        stage.whenComplete((value, failure) -> {
            try {
                if (failure == null) {
                    envelope.result().complete(value);
                } else {
                    envelope.result().completeExceptionally(failure);
                }
            } finally {
                inFlight.release();
            }
        });
    }

    public int queued() {
        return queue.size();
    }

    public boolean awaitDrained(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while ((!queue.isEmpty() || inFlight.availablePermits() != maxInFlight)
                && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(1);
        }
        return queue.isEmpty() && inFlight.availablePermits() == maxInFlight;
    }

    @Override
    public void close() {
        if (accepting.compareAndSet(true, false)) {
            pump.interrupt();
            try {
                pump.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            invocationExecutor.shutdown();
            try {
                if (!invocationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    invocationExecutor.shutdownNow();
                }
            } catch (InterruptedException interrupted) {
                invocationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
