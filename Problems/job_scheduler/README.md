# Job / Task Scheduler

## Interview Prompt

Design an in-process scheduler that accepts one-time or recurring tasks, runs ready work on bounded workers, records outcomes, and shuts down without losing accepted tasks.

## Scope and Requirements

1. Schedule a task for an instant or a fixed-delay/fixed-rate recurrence.
2. Cancel a task before execution and expose current status.
3. Order tasks by next run time, then a stable sequence for ties.
4. Bound queued and running work; define rejection/backpressure explicitly.
5. Retry retryable failures according to a policy and dead-letter exhausted tasks.
6. Support graceful shutdown: reject new work, finish or cancel accepted work by policy.

## Core Model

| Type | Responsibility |
| --- | --- |
| `ScheduledTask` | Identity, executable command, next run, recurrence, and version |
| `TaskState` | `SCHEDULED`, `CLAIMED`, `RUNNING`, terminal states |
| `SchedulePolicy` | Computes the next execution instant |
| `ReadyQueue` | Time-ordered delay queue |
| `WorkerPool` | Bounded execution capacity |
| `RetryPolicy` | Classifies failure and computes retry time |
| `TaskRepository` | Durable task and attempt state when persistence is required |

## Invariants

- One logical occurrence is claimed by at most one worker.
- A cancelled occurrence never transitions back to running.
- Fixed-rate scheduling is based on the intended schedule; fixed-delay uses completion time.
- Retries have unique attempt identities and never create two next occurrences.
- Shutdown state only moves forward: `RUNNING → DRAINING → TERMINATED`.

## API Sketch

```java
TaskId schedule(TaskCommand command, Instant runAt, String idempotencyKey);
TaskId scheduleRecurring(TaskCommand command, SchedulePolicy policy, String key);
boolean cancel(TaskId id, long expectedVersion);
TaskSnapshot status(TaskId id);
void shutdown(ShutdownMode mode, Duration timeout);
```

## Concurrency Solution

A coordinator waits on a delay queue and atomically claims due tasks. Claimed occurrences enter a bounded worker pool. Persist `CLAIMED` before dispatch in a durable design and lease the claim so another scheduler can recover it after a crash. Completion writes the attempt result and, for recurring work, the next occurrence in one transaction. Inject `Clock` for deterministic tests. Never hold scheduler locks while running user code.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/Scheduler/` — Java job/status/data-service fragment. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Scheduler)
2. `References/kumaransg-LLD/Low_level_Problem_set_2/TaskSchedulerLLD/` — custom Java scheduler and scheduled-task model. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/TaskSchedulerLLD)
3. `References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/jobscheduling/` — task-to-machine scheduling variation, a different resource-allocation interpretation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/jobscheduling)

## Follow-Ups

Cron expressions, distributed leases, priority/fairness, dependency DAGs, misfire policies, per-tenant quotas, and exactly-once effect limitations.

## Implementation Status

This is the canonical interview solution; the three differently scoped Java sources remain available in the exact local clone.
