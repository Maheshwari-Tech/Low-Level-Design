# Concurrency Question Bank

Each exercise must have a deterministic test, clean termination, and no correctness dependency on `Thread.sleep`. State the protected invariant and linearization point before choosing primitives.

Use the canonical [Java Multithreading and Concurrency Interview Guide](../JAVA_MULTITHREADING_INTERVIEW_GUIDE.md) for the concepts and interview decision framework. The [Java 17 interview lab](../interview/) is the executable baseline for `ExecutorService`, bounded `BlockingQueue`, `Semaphore`, `synchronized`, and `ReentrantLock`; these exercises extend that baseline rather than repeat it.

## Coordination exercises

### Print FooBar alternately

Two methods are called by different threads. Produce `foobar` exactly `n` times, always alternating. Handle `n = 0`, interruption, and either start order.

### Print Zero, Even, Odd

Three threads print zero, odd numbers, and even numbers to form `010203...0n`. No thread may print out of turn, and all threads must terminate if one is cancelled.

### Multithreaded FizzBuzz

Four workers handle fizz, buzz, fizzbuzz, and plain-number cases for `1..n`. Each value is emitted exactly once and in numeric order.

### Build H2O

Hydrogen and oxygen worker calls arrive in any order. Release exactly two hydrogen calls and one oxygen call per molecule; do not let atoms from an unbounded number of incomplete molecules pass the barrier.

### Producer-consumer blocking queue

Implement a bounded generic queue with blocking `put` and `take`, optional timed operations, interruption, and shutdown semantics. Avoid lost wake-ups and signal only when a relevant predicate may have changed. Review the [legacy local implementations](../com/example/lld/concurrency/code/producer_consumer/) as defect-finding exercises: one explicit-lock version busy-spins without `Condition` or `finally`, while the semaphore version can leak permits and relies on a nonstandard custom semaphore.

## Concurrent data structures

### Thread-safe cache with TTL

Support `get`, `put`, `remove`, capacity eviction, and per-entry expiry using an injectable clock. Define whether an expired read removes the entry, how cleanup runs, and how duplicate loads for one key are coalesced.

### Concurrent hash map

Implement `get`, `put`, `remove`, and resize. Define null handling, atomic replacement, visibility, and how operations proceed during resize. Start with coarse locking, then justify lock striping or another refinement.

### Concurrent Bloom filter

Support concurrent `add` and `mightContain` over a fixed bit array. Explain atomic bit updates, false-positive probability, hash derivation, and why deletions require a different structure such as a counting Bloom filter.

## Parallel algorithm

### Multithreaded merge sort

Split work recursively, sort independent partitions in a bounded executor or fork/join pool, and merge deterministically. Use a sequential threshold and show that failures, cancellation, and pool shutdown do not leak tasks.

## Additional local exercise

### Dining philosophers

Several philosophers alternate between thinking and eating, but each meal requires two adjacent forks. Prevent deadlock, avoid starvation under the declared fairness policy, bound resource ownership, and support cancellation. Compare lock ordering, waiter/arbitrator, and semaphore-based solutions.

### Unisex bathroom

Coordinate two groups sharing a capacity-limited resource: occupants from opposing groups may not overlap, capacity is bounded, and neither group should starve. Document the fairness policy and test adversarial arrival sequences. Treat the [legacy local implementation](../com/example/lld/concurrency/code/unisexBathroom/) as a review exercise: it lacks bounded fairness, does not put permit/occupancy cleanup in `finally`, and uses sleep-based demonstration rather than deterministic phase control.
