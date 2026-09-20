package com.example.lld.concurrency.interview;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Deterministic, interview-sized demonstrations of the Java concurrency tools
 * that commonly appear in low-level-design rounds.
 */
public final class ConcurrencyInterviewLab {
    private static final long TIMEOUT_SECONDS = 5L;

    private ConcurrencyInterviewLab() {
    }

    public static void main(String[] args) throws Exception {
        demonstrateExecutorLifecycleAndFailure();
        demonstrateBoundedQueueBackpressure();
        demonstrateSemaphorePermitSafety();
        demonstrateCompoundInvariantProtection();
        System.out.println("All concurrency interview scenarios passed.");
    }

    private static void demonstrateExecutorLifecycleAndFailure() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Integer> answer = executor.submit(() -> 6 * 7);
        Future<Void> failed = executor.submit(() -> {
            throw new IllegalStateException("worker failed");
        });

        try {
            require(answer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) == 42,
                    "the successful Future must carry its result");

            try {
                failed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                throw new AssertionError("a failed task must fail its Future");
            } catch (ExecutionException expected) {
                require(expected.getCause() instanceof IllegalStateException,
                        "Future.get must expose the worker failure as its cause");
            }
        } finally {
            shutdownAndAwait(executor);
        }

        require(executor.isTerminated(), "the executor must not leak worker threads");
        System.out.println("PASS ExecutorService: result, failure propagation, and shutdown");
    }

    private static void demonstrateBoundedQueueBackpressure() throws Exception {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
        CountDownLatch firstItemStored = new CountDownLatch(1);
        CountDownLatch secondPutStarted = new CountDownLatch(1);
        CountDownLatch allowConsumer = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Void> producer = executor.submit(() -> {
            queue.put(10);
            firstItemStored.countDown();
            secondPutStarted.countDown();
            queue.put(20); // Cannot complete until the consumer frees capacity.
            return null;
        });
        Future<List<Integer>> consumer = executor.submit(() -> {
            allowConsumer.await();
            return List.of(queue.take(), queue.take());
        });

        try {
            await(firstItemStored, "producer did not store the first item");
            await(secondPutStarted, "producer did not attempt the second put");
            require(queue.remainingCapacity() == 0, "the bounded queue must be full");
            require(!producer.isDone(), "a producer must wait while the queue is full");

            allowConsumer.countDown();
            require(producer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) == null,
                    "the producer must finish once capacity is available");
            require(consumer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).equals(List.of(10, 20)),
                    "the consumer must observe FIFO order");
        } finally {
            allowConsumer.countDown();
            shutdownAndAwait(executor);
        }

        System.out.println("PASS BlockingQueue: FIFO hand-off and bounded backpressure");
    }

    private static void demonstrateSemaphorePermitSafety() throws Exception {
        int concurrencyLimit = 2;
        Semaphore semaphore = new Semaphore(concurrencyLimit, true);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maximumInside = new AtomicInteger();
        CountDownLatch twoWorkersEntered = new CountDownLatch(concurrencyLimit);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Integer>> workers = new ArrayList<>();

        for (int workerId = 0; workerId < 3; workerId++) {
            int id = workerId;
            workers.add(executor.submit(() -> {
                boolean acquired = false;
                boolean countedInside = false;
                try {
                    semaphore.acquire();
                    acquired = true;

                    int currentInside = inside.incrementAndGet();
                    countedInside = true;
                    maximumInside.accumulateAndGet(currentInside, Math::max);
                    twoWorkersEntered.countDown();
                    releaseWorkers.await();

                    if (id == 1) {
                        throw new IllegalStateException("resource call failed");
                    }
                    return id;
                } finally {
                    if (countedInside) {
                        inside.decrementAndGet();
                    }
                    if (acquired) {
                        semaphore.release();
                    }
                }
            }));
        }

        try {
            await(twoWorkersEntered, "two workers did not acquire permits");
            require(inside.get() == concurrencyLimit, "exactly two workers should be inside");
            require(semaphore.availablePermits() == 0, "both permits should be owned");
            releaseWorkers.countDown();

            int failures = 0;
            for (Future<Integer> worker : workers) {
                try {
                    worker.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (ExecutionException expected) {
                    require(expected.getCause() instanceof IllegalStateException,
                            "the worker must preserve its original failure");
                    failures++;
                }
            }

            require(failures == 1, "exactly one resource call should fail");
            require(maximumInside.get() == concurrencyLimit,
                    "the semaphore must cap concurrent resource use");
            require(inside.get() == 0, "all workers must leave the guarded region");
            require(semaphore.availablePermits() == concurrencyLimit,
                    "finally must return every acquired permit, including on failure");
        } finally {
            releaseWorkers.countDown();
            shutdownAndAwait(executor);
        }

        System.out.println("PASS Semaphore: concurrency cap and exception-safe permits");
    }

    private static void demonstrateCompoundInvariantProtection() throws Exception {
        exerciseLedger("synchronized", new SynchronizedCapacityLedger(3));
        exerciseLedger("ReentrantLock", new LockingCapacityLedger(3));
    }

    private static void exerciseLedger(String implementation, CapacityLedger ledger)
            throws Exception {
        int contenders = 8;
        int capacity = 3;
        CountDownLatch allReady = new CountDownLatch(contenders);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        List<Future<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < contenders; i++) {
            attempts.add(executor.submit(() -> {
                allReady.countDown();
                startTogether.await();
                return ledger.reserve();
            }));
        }

        try {
            await(allReady, implementation + " contenders did not become ready");
            startTogether.countDown();

            int reservations = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    reservations++;
                }
            }

            LedgerSnapshot full = ledger.snapshot();
            require(reservations == capacity, implementation + " must not oversell capacity");
            require(full.available() == 0 && full.reserved() == capacity,
                    implementation + " must update both fields atomically");

            List<Future<?>> releases = new ArrayList<>();
            for (int i = 0; i < reservations; i++) {
                releases.add(executor.submit(ledger::release));
            }
            for (Future<?> release : releases) {
                release.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }

            LedgerSnapshot empty = ledger.snapshot();
            require(empty.available() == capacity && empty.reserved() == 0,
                    implementation + " must restore the invariant after release");
        } finally {
            startTogether.countDown();
            shutdownAndAwait(executor);
        }

        System.out.println("PASS " + implementation
                + ": available + reserved remains equal to capacity");
    }

    private interface CapacityLedger {
        boolean reserve();

        void release();

        LedgerSnapshot snapshot();
    }

    private record LedgerSnapshot(int available, int reserved) {
    }

    /** Monitor-based version: the object's monitor owns the entire state transition. */
    private static final class SynchronizedCapacityLedger implements CapacityLedger {
        private final int capacity;
        private int available;
        private int reserved;

        private SynchronizedCapacityLedger(int capacity) {
            requirePositive(capacity);
            this.capacity = capacity;
            this.available = capacity;
        }

        @Override
        public synchronized boolean reserve() {
            if (available == 0) {
                return false;
            }
            available--;
            reserved++;
            verifyInvariant();
            return true;
        }

        @Override
        public synchronized void release() {
            requireReservation();
            reserved--;
            available++;
            verifyInvariant();
        }

        @Override
        public synchronized LedgerSnapshot snapshot() {
            return new LedgerSnapshot(available, reserved);
        }

        private void requireReservation() {
            if (reserved == 0) {
                throw new IllegalStateException("nothing is reserved");
            }
        }

        private void verifyInvariant() {
            if (available < 0 || reserved < 0 || available + reserved != capacity) {
                throw new IllegalStateException("capacity invariant violated");
            }
        }
    }

    /** Explicit-lock version: same invariant, with scope made visible by try/finally. */
    private static final class LockingCapacityLedger implements CapacityLedger {
        private final Lock lock = new ReentrantLock();
        private final int capacity;
        private int available;
        private int reserved;

        private LockingCapacityLedger(int capacity) {
            requirePositive(capacity);
            this.capacity = capacity;
            this.available = capacity;
        }

        @Override
        public boolean reserve() {
            lock.lock();
            try {
                if (available == 0) {
                    return false;
                }
                available--;
                reserved++;
                verifyInvariant();
                return true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void release() {
            lock.lock();
            try {
                if (reserved == 0) {
                    throw new IllegalStateException("nothing is reserved");
                }
                reserved--;
                available++;
                verifyInvariant();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public LedgerSnapshot snapshot() {
            lock.lock();
            try {
                return new LedgerSnapshot(available, reserved);
            } finally {
                lock.unlock();
            }
        }

        private void verifyInvariant() {
            if (available < 0 || reserved < 0 || available + reserved != capacity) {
                throw new IllegalStateException("capacity invariant violated");
            }
        }
    }

    private static void requirePositive(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
    }

    private static void await(CountDownLatch latch, String failureMessage)
            throws InterruptedException {
        require(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), failureMessage);
    }

    private static void shutdownAndAwait(ExecutorService executor)
            throws InterruptedException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                List<Runnable> cancelled = executor.shutdownNow();
                throw new AssertionError("executor did not terminate; cancelled "
                        + cancelled.size() + " queued task(s)");
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
