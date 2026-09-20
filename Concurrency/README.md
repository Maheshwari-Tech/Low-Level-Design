# Concurrency and Multithreading

Concurrency is the coordination of tasks whose lifetimes overlap. Parallelism is the simultaneous execution of work. A concurrent design may run on one core; a parallel algorithm may still require coordination to combine results.

## Fundamentals

- A **process** owns an isolated address space and resources. Threads in one process share heap state but have independent stacks.
- A thread moves through creation, runnable/running, waiting or blocked, and termination states.
- A **race condition** occurs when correctness depends on an uncontrolled interleaving. The code region protecting an invariant is its critical section.
- Memory visibility matters as much as atomicity: one thread's write is not automatically observed by another at the required time.

See [Multithreading introduction](Multithreading/Introduction.md) for the Java execution and memory-model notes.

For the complete interview roadmap—including Python-to-Java mapping, `synchronized`, executors, blocking queues, semaphores, coordination utilities, concurrent collections, cancellation, testing, and LLD applications—use the [Java Multithreading and Concurrency Interview Guide](JAVA_MULTITHREADING_INTERVIEW_GUIDE.md). Then run the [Java 17 interview lab](interview/) for compact, deterministic implementations of the four core primitives before attempting the [question bank](questions/).

## Coordination primitives

- **Mutex/monitor:** one owner enters a critical section at a time.
- **Semaphore:** a fixed number of permits control access to a resource.
- **Condition variable:** wait for a predicate while atomically releasing and reacquiring a lock.
- **Reentrant lock:** the owning thread can acquire the same lock repeatedly; explicit locks can also offer timed or interruptible acquisition.
- **Read-write lock:** allows concurrent readers while serializing writers when the workload benefits.
- **Latch, barrier, future:** coordinate completion, phases, or a result that arrives later.
- **Compare-and-swap and atomics:** perform conditional updates without a traditional lock; the surrounding algorithm still needs a proof of correctness.

Prefer the highest-level primitive that expresses the invariant. Coarse-grained locking is easier to reason about but limits concurrency; fine-grained locking increases throughput potential and deadlock risk. `tryLock` or a timed acquire can bound waiting but does not repair an invalid locking protocol.

## Failure modes

- **Deadlock:** participants wait forever in a cycle. Prevent it with consistent lock ordering, limited lock scope, or avoiding hold-and-wait.
- **Livelock:** participants keep reacting but make no progress. Add arbitration or randomized/backoff behavior.
- **Starvation:** one participant is repeatedly denied progress. Consider fairness and bounded work.
- **Lost wake-up:** a condition is signaled without the required predicate/lock protocol. Always wait in a loop that rechecks the predicate.

## Reusable patterns

- Signaling and guarded suspension
- Producer-consumer with a bounded blocking queue
- Thread pool and executor service
- Reader-writer coordination
- Immutable snapshot and copy-on-write state
- Lock striping for mostly independent keys

## Legacy code-review exercises

The older local samples are preserved as review material, not presented as canonical interview solutions:

| Sample | What to inspect before reusing it |
| --- | --- |
| [Producer-consumer queues](com/example/lld/concurrency/code/producer_consumer/) | `BlockingQueueWithLockImpl` repeatedly unlocks and relocks while full or empty, so it busy-spins instead of waiting on a `Condition`, and its lock release is not protected by `finally`. `BlockingQueueWithSemaphoreImpl` can leak permits when an acquire or mutation fails; its custom `CountingSemaphore.release` blocks at zero, unlike `java.util.concurrent.Semaphore.release`. The demo also uses `Thread.sleep` for coordination. |
| [Unisex bathroom](com/example/lld/concurrency/code/unisexBathroom/) | The implementation has no bounded-fairness policy and may starve one group. Permit and occupancy cleanup are not in `finally`, so interruption or failure can leak capacity/state; the demo uses sleeps rather than phase coordination. |

Use the [interview lab](interview/) as the corrected executable baseline. A good review exercise is to state the invariant and termination policy, replace spinning or sleep-based coordination, make cleanup exception-safe, and add deterministic contention tests.

## Practice

The [concurrency question bank](questions/) covers ordered printing, multithreaded FizzBuzz, H2O coordination, TTL caches, concurrent maps and Bloom filters, blocking queues, and parallel merge sort.

For every solution, document the protected invariant, linearization point, lock order, termination protocol, interruption behavior, and a test strategy that does not rely on sleeps.
