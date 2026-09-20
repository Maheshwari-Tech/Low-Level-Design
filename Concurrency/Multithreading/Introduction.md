# Java Multithreading Introduction

## Processes, threads, and scheduling

A process owns resources and an address space. Threads inside it share heap objects but each thread has its own call stack. The scheduler time-slices runnable threads; a context switch pauses one execution state and resumes another. More threads can improve responsiveness or resource utilization, but they also add scheduling, memory, and coordination cost.

## Thread safety

Code is thread-safe when it preserves its invariants for every supported interleaving. Common approaches are:

- avoid shared mutable state through immutability or confinement;
- serialize access with `synchronized` or `Lock`;
- use concurrent collections whose compound-operation guarantees match the use case;
- perform a simple state change with an atomic type;
- publish immutable snapshots through a safe visibility mechanism.

`volatile` provides visibility and ordering for reads and writes of one variable; it does not make a read-modify-write sequence such as `count++` atomic.

## Wait and notification

Busy waiting consumes CPU while repeatedly checking a condition. A monitor or `Condition` lets a thread wait until another thread may have changed the predicate.

Always protect the predicate with the same lock and recheck it in a loop:

```java
synchronized (queue) {
    while (queue.isEmpty()) {
        queue.wait();
    }
    return queue.removeFirst();
}
```

The loop handles spurious wake-ups and competition from another awakened thread. State change and notification must follow one consistent locking protocol.

## Executors

Prefer an `ExecutorService` to manually creating a thread per task. It separates task submission from worker management, bounds resource use, and provides orderly shutdown. Define what happens to queued and running tasks during shutdown and propagate cancellation or interruption deliberately.

## Review questions

1. What exact state is shared?
2. Which invariant spans more than one field or operation?
3. Where is the linearization point of each public command?
4. Can locks be acquired in different orders?
5. How do waiting threads terminate or respond to interruption?
6. Can immutable state or partitioning remove the need for a lock?

Continue with the canonical [Java Multithreading and Concurrency Interview Guide](../JAVA_MULTITHREADING_INTERVIEW_GUIDE.md), run the deterministic [Java 17 interview lab](../interview/), and then apply the reasoning to the [practice questions](../questions/). The [concurrency index](../README.md) remains the short topic map and legacy-sample review entry point.
