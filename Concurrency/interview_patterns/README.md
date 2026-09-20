# Java Concurrency Patterns for LLD Interviews

This runnable Java 17 example combines the cross-cutting mechanics most likely to
differentiate a strong Senior/Staff LLD answer: asynchronous composition, bounded
producer-consumer dispatch, backpressure, selective retries, and Observer-style events.

Use it with the featured commerce problems:

- Order and payment use `AsyncRetry` only for transient, idempotent provider commands.
- Inventory uses a lock or database transaction for the stock invariant; an event bus cannot
  make a read-check-write sequence atomic.
- Notification uses `BoundedAsyncDispatcher` to decouple producers from provider I/O.
- Warehouse can use the dispatcher for pick/status jobs and `EventBus` for local projections.
- Rate limiter state still requires a per-key atomic operation; asynchronous dispatch is not a
  substitute for synchronization.

## Components

### `AsyncRetry`

`AsyncRetry.execute` accepts a supplier of `CompletionStage<T>`. An unsuccessful attempt schedules
the next one on `ScheduledExecutorService` and returns the completion thread immediately. It never
calls `Thread.sleep()` and never blocks with `get()` or `join()` internally.

Important guarantees and limitations:

- One result future represents all attempts.
- Synchronous exceptions and failed stages follow the same path.
- `CompletionException`/`ExecutionException` are unwrapped before classification.
- Only the supplied predicate decides whether an error is retryable.
- Exponential delay is capped and multiplication is overflow-safe.
- Production code should add jitter, a total deadline, cancellation propagation, metrics, and a
  circuit breaker where appropriate.
- A payment or notification attempt must retain the same provider idempotency key. Retrying with a
  new key can duplicate the external effect.

### `BoundedAsyncDispatcher`

Producers call `submit` and receive a `CompletionStage<R>`. `ArrayBlockingQueue` gives the system a
finite admission buffer. A pump removes accepted envelopes, and a `Semaphore` bounds asynchronous
work already sent to the handler. Completion—success or failure—finishes the producer's future and
releases the permit.

The design avoids two common problems:

1. An unbounded queue eventually turns provider slowdown into an out-of-memory failure.
2. Calling `future.join()` inside a worker wastes threads while remote I/O is outstanding.

A full queue fails fast with `RejectedExecutionException`. A production API may instead block with
a deadline, shed low-priority work, return HTTP 429/503, or publish to a durable broker. That choice
is a business backpressure policy, not an implementation detail.

The local queue is deliberately not durable. Accepted-but-unprocessed work is lost on process
failure. For business notifications or orders, persist the command and an outbox entry in one local
transaction, then let workers consume at least once. The consumer makes the business effect
idempotent.

### `EventBus`

`EventBus` is an in-process asynchronous Observer implementation:

- Subscriber storage uses `CopyOnWriteArrayList`, appropriate when publish is frequent and
  subscribe/unsubscribe is rare.
- A subscription returns `AutoCloseable` so lifecycle and memory retention are explicit.
- `publish` returns a stage that completes only when all observers complete; observer exceptions
  therefore remain visible.

It does not provide durability, ordering across threads, replay, or cross-process delivery. For a
domain fact that must survive a crash, commit an outbox record with the aggregate and publish it at
least once. Consumers deduplicate by event ID and aggregate version.

## Main sequence

```text
Producer
  -> bounded queue
  -> pump acquires in-flight permit
  -> handler starts provider request
       -> timeout
       -> scheduler starts same idempotent request after backoff
       -> provider accepts
  -> asynchronous event observers
  -> producer result completes
  -> in-flight permit released
```

## Interview discussion

### Why not `synchronized` everywhere?

`synchronized` is a valid starting point for a small in-memory aggregate, but one global monitor
serializes unrelated keys. Choose the narrowest boundary that protects the invariant: per-payment
aggregate, per-rate-limit key, deterministic multi-SKU locks, or one database transaction.

### Why `CompletionStage` rather than returning a value?

Remote payment, carrier, and messaging calls have independent latency and failure. Returning a
stage makes pending completion explicit and lets callers compose success, compensation, timeout,
and observation without occupying a waiting thread.

### Does an async API make the implementation thread-safe?

No. Asynchrony is about scheduling and waiting; thread safety is about shared mutable state. A
continuation can run on any completion thread. Aggregates still need confinement, locks,
compare-and-set, or transactional conditional writes.

### What should be retried?

Retry transient transport errors, throttling after the provider's `Retry-After`, and explicitly
documented retryable server failures. Do not retry validation errors, permission failures, card
declines, or non-idempotent commands. A timeout often means unknown outcome, so reconcile by the
stable external reference rather than assuming failure.

### What is effectively-once processing?

Transport normally delivers at least once. A stable command/event ID, a durable idempotency receipt
or unique constraint, and one transaction around the domain mutation make duplicate deliveries
produce one business effect. Calling this exactly-once delivery is misleading.

## Build and run

```bash
cd Concurrency/interview_patterns
rm -rf out
mkdir -p out
find src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -d out
java -cp out com.example.lld.concurrency.interview_patterns.Demo
```

Expected output:

```text
Concurrency interview patterns demo passed
  non-blocking attempts: 2
  observed events: 1
```

The demo fails fast with `AssertionError` and proves that the first timeout triggers exactly one
scheduled retry, the producer receives the provider result, the observer sees one event, and the
bounded dispatcher drains all in-flight work.
