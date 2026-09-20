# Java 17 Concurrency Interview Lab

This lab is the amount of code worth writing in a one-hour LLD interview. Each scenario states a protected property, uses a standard library primitive, controls thread phases with latches, and shuts every executor down. It deliberately has no `Thread.sleep` correctness dependency.

## Run it

From `Concurrency/interview`:

```bash
mkdir -p /tmp/lld-concurrency-interview
javac --release 17 -Xlint:all \
  -d /tmp/lld-concurrency-interview \
  src/com/example/lld/concurrency/interview/ConcurrencyInterviewLab.java
java -ea -cp /tmp/lld-concurrency-interview \
  com.example.lld.concurrency.interview.ConcurrencyInterviewLab
```

Expected result:

```text
PASS ExecutorService: result, failure propagation, and shutdown
PASS BlockingQueue: FIFO hand-off and bounded backpressure
PASS Semaphore: concurrency cap and exception-safe permits
PASS synchronized: available + reserved remains equal to capacity
PASS ReentrantLock: available + reserved remains equal to capacity
All concurrency interview scenarios passed.
```

## What each scenario proves

| Scenario | Core interview point | Property demonstrated |
|---|---|---|
| `ExecutorService` and `Future` | Submit units of work instead of manually owning threads; observe results and failures; always define shutdown ownership. | A successful task returns through `Future.get`; a task exception is preserved as the cause of `ExecutionException`; `shutdown` plus `awaitTermination` prevents a thread leak. |
| Bounded `BlockingQueue` | Let the queue implement the producer-consumer wait protocol and use capacity as load regulation. | With capacity one, the second `put` cannot complete until a `take` frees space. The queue also preserves FIFO hand-off. |
| `Semaphore` | Model a pool of permits, not ownership of shared mutable state. Acquire before entry and release in `finally` only if acquisition succeeded. | At most two calls enter the simulated resource, and every permit returns even when one call throws. |
| `synchronized` ledger | Use the object's monitor when one lock and unconditional acquisition are sufficient. | `available--` and `reserved++` form one atomic transition, so concurrent callers cannot oversell. |
| `ReentrantLock` ledger | Use an explicit lock when timed/interruptible acquisition, multiple conditions, or explicit lock operations are genuinely needed. | The same compound invariant is protected with visible `lock`/`try`/`finally` scope. |

## How to explain the code in an interview

For every shared component, say these five things before typing:

1. **Invariant:** for the ledger, `available >= 0`, `reserved >= 0`, and `available + reserved == capacity`.
2. **Critical section:** the check and both field updates must happen under the same monitor or lock.
3. **Waiting policy:** the queue blocks producers only while full; the semaphore blocks while no permit is available.
4. **Failure policy:** worker failures cross the executor boundary through `Future`; permits and locks are returned in `finally`; interruption is not swallowed.
5. **Lifecycle:** the component that creates an executor owns its shutdown, draining or cancelling according to product requirements.

The latches in this lab are test coordination, not production business logic. They make the relevant interleaving reproducible: first establish that a worker has reached a phase, then let the next phase proceed.

## Senior and Staff follow-ups

- **Overload:** a bounded queue applies backpressure, but a request-facing service may prefer timed `offer`, rejection, shedding, or admission control instead of blocking indefinitely.
- **Cancellation:** decide whether cancelling a `Future` should interrupt running work and ensure the task treats interruption as cooperative cancellation.
- **Fairness:** fair semaphores and locks reduce barging but can reduce throughput; fairness does not automatically guarantee an application-level SLA.
- **Sizing:** separate CPU-bound and blocking I/O pools; derive sizes from cores, wait/compute ratio, downstream limits, and measured saturation rather than using one global pool.
- **Visibility:** locking provides both mutual exclusion and a happens-before edge. `volatile` gives visibility for a variable but cannot make a multi-field state transition atomic.
- **Distributed boundary:** JVM locks protect one process only. Cross-instance inventory or quota correctness needs a database constraint/transaction, partition ownership, or a distributed coordination design.
