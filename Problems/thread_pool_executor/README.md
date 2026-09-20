# Custom Thread Pool Executor

## Interview Prompt

Design a bounded thread pool that accepts tasks, executes them on reusable workers, supports rejection and shutdown policies, and exposes task outcomes without races or lost wake-ups.

## Requirements

1. Start a configured minimum/maximum number of worker threads.
2. Queue submitted tasks up to a bounded capacity.
3. Return a future/handle with completion, failure, and cancellation state.
4. Apply an explicit rejection policy when capacity is exhausted.
5. Support orderly shutdown and immediate shutdown with documented guarantees.
6. Isolate task exceptions so one failure does not kill a worker silently.

## Model and State

| Type | Responsibility |
| --- | --- |
| `ThreadPool` | Lifecycle, worker set, queue, and submission boundary |
| `WorkQueue` | Blocking bounded queue with close semantics |
| `Worker` | Take-run-report loop that never holds the pool lock during user code |
| `TaskHandle<T>` | `QUEUED → RUNNING → SUCCEEDED/FAILED/CANCELLED` |
| `RejectionPolicy` | Abort, caller-runs, discard, or timed backpressure |
| `PoolState` | `RUNNING → SHUTDOWN/STOP → TERMINATED` |

## Invariants

- Every accepted task reaches at most one terminal state.
- A queued task is removed by either one worker or successful cancellation, never both.
- No new task is accepted after shutdown begins.
- Worker count stays inside configured bounds.
- Pool locks are not held while arbitrary task code executes or callbacks run.

## API Sketch

```java
<T> TaskHandle<T> submit(Callable<T> task);
void shutdown();
List<Runnable> shutdownNow();
boolean awaitTermination(Duration timeout) throws InterruptedException;
PoolSnapshot snapshot();
```

## Concurrency Solution

Guard lifecycle and worker membership with one lock; use a condition-based bounded queue for work. Submission checks state and either enqueues, starts a worker, or invokes the rejection policy. A worker loops on `take`, atomically marks the handle running, executes outside locks, records outcome, and then decides whether to retire. Shutdown closes queue admission and wakes all waiters. `shutdownNow` interrupts workers and drains tasks that never started. Use `while` around every condition wait and preserve interrupt status when policy requires it.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Problem_set_2/customThreadPoolExecutor/` — an incomplete Java study fragment with an unbounded `ArrayList` task store and reusable workers. It has no task result/state handle, rejection policy, cancellation, or shutdown protocol, so use it as code-review input rather than as the solution. The same folder also contains a separate polling-based scheduler fragment. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/customThreadPoolExecutor)

## Essential Tests

One-winner cancellation race, bounded rejection, thrown task reuse, graceful drain, immediate interrupt, spurious wake-up resistance, and no thread leak after termination.

## Implementation Status

This canonical page completes the interview contract and concurrency reasoning; the actual Java variation remains in the exact local clone.
