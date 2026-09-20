# Java Multithreading and Concurrency for LLD Interviews

This guide focuses on what you need to **understand, explain, design, and code** in a Java Low-Level Design interview. The goal is not to memorize every class in `java.util.concurrent`; it is to identify the shared invariant, choose the smallest correct concurrency primitive, define lifecycle behavior, and explain the trade-offs.

## 1. What interviewers expect

For an LLD problem involving concurrency, a strong candidate can:

1. Identify shared mutable state and the invariant that must remain true.
2. Explain the race rather than merely adding `synchronized` everywhere.
3. Choose between confinement, immutability, atomics, locks, queues, and executors.
4. Define task admission, backpressure, cancellation, shutdown, and failure handling.
5. Avoid deadlock, starvation, busy waiting, and lost notifications.
6. State the linearization point of important operations.
7. Test competing operations without relying only on `Thread.sleep`.

The most important interview sentence is:

> “This state is shared, this invariant must be atomic, and this operation is the linearization point.”

## 2. Python-to-Java mental map

If you already know Python concurrency, use this mapping:

| Python | Java | Important difference |
| --- | --- | --- |
| `threading.Thread` | `Thread` | Prefer executors for submitted work in both languages. |
| `concurrent.futures.ThreadPoolExecutor` | `ExecutorService` / `ThreadPoolExecutor` | Java exposes queue, rejection, pool-size, and shutdown policies directly. |
| `queue.Queue` | `BlockingQueue` | Java provides several bounded, priority, delay, and handoff implementations. |
| `threading.Lock` | `synchronized` or `Lock` | Java intrinsic locks also establish memory-visibility guarantees. |
| `threading.RLock` | `ReentrantLock` | Both allow the owner to acquire the same lock repeatedly. |
| `threading.Semaphore` | `Semaphore` | Java optionally supports fair permit ordering. |
| `threading.Condition` | `Condition` or `wait/notifyAll` | The predicate must be protected by the associated lock. |
| `threading.Event` | `CountDownLatch(1)` or a volatile flag | Choose based on one-shot signaling versus repeatedly changing state. |
| `threading.Barrier` | `CyclicBarrier` or `Phaser` | `Phaser` supports changing participant counts and multiple phases. |
| `Future` | `Future`, `CompletableFuture` | `CompletableFuture` supports non-blocking composition. |

Do not carry Python’s Global Interpreter Lock assumptions into Java. Java threads can execute Java code truly in parallel on multiple cores, so unsynchronized shared state can race even in CPU-bound code.

## 3. Foundation concepts

### Concurrency versus parallelism

- **Concurrency:** multiple tasks make progress during overlapping lifetimes.
- **Parallelism:** multiple tasks execute simultaneously on different cores.
- A concurrent design can run on one core; parallel execution still requires correct coordination.

### Process versus thread

- A process owns an isolated address space and operating-system resources.
- Threads in one process share heap objects and process resources.
- Each thread has its own stack and instruction position.
- Threads are cheaper than processes but shared memory creates race and visibility risks.

### Thread lifecycle

Know the Java states conceptually:

- `NEW`
- `RUNNABLE`
- `BLOCKED` while waiting to enter a monitor
- `WAITING`
- `TIMED_WAITING`
- `TERMINATED`

Calling `thread.start()` schedules a new thread. Calling `thread.run()` directly is an ordinary method call on the current thread.

### Runnable, Callable, Future

- `Runnable`: no returned value and cannot declare checked exceptions.
- `Callable<T>`: returns a value and can throw an exception.
- `Future<T>`: represents a submitted computation; supports waiting, cancellation, and result retrieval.
- `CompletableFuture<T>`: supports pipelines, fan-out/fan-in, recovery, and asynchronous composition.

## 4. The Java Memory Model: the essential interview version

Concurrency correctness has three separate dimensions:

1. **Atomicity:** an operation appears indivisible.
2. **Visibility:** one thread observes another thread’s writes.
3. **Ordering:** reads and writes are not observed in an invalid order.

### Happens-before rules to know

If A happens-before B, B must observe A’s effects. Important examples:

- Unlocking a monitor happens-before a later lock of the same monitor.
- A write to a `volatile` variable happens-before a later read of that variable.
- Actions before `Thread.start()` are visible to the started thread.
- All actions in a thread happen-before another thread successfully returns from `join()`.
- Actions before submitting a task to an executor happen-before that task executes.
- Task actions happen-before another thread obtains the result through `Future.get()`.
- Actions before inserting an element into a concurrent queue happen-before actions
  after another thread removes that element.

### `volatile`

Use `volatile` when one variable represents independently readable state, such as a stop flag or immutable configuration reference.

```java
final class Worker implements Runnable {
    private volatile boolean running = true;

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            doOneUnitOfWork();
        }
    }

    private void doOneUnitOfWork() {
        // bounded work
    }
}
```

`volatile` does **not** make compound operations atomic:

```java
volatile int count;
count++; // read + add + write; still races
```

Use `AtomicInteger.incrementAndGet()`, a lock, or confinement instead.

### Safe publication

An object should not become visible to other threads before construction finishes. Safe approaches include:

- storing it in a `final` field during construction;
- publishing it through a properly locked field;
- writing the reference to a `volatile` field;
- placing it into a concurrent collection;
- completing a future with it;
- using static initialization.

Avoid starting a thread from a constructor because `this` may escape before construction completes.

## 5. `synchronized`: the default mutual-exclusion tool

Every Java object has an intrinsic monitor lock. `synchronized` provides:

- mutual exclusion;
- reentrancy;
- automatic unlock when the block exits, including by exception;
- visibility and ordering at lock/unlock boundaries.

Know exactly which monitor is acquired:

| Form | Monitor |
| --- | --- |
| `synchronized` instance method | `this` |
| `static synchronized` method | the declaring `Class` object |
| `synchronized (lock)` block | the evaluated `lock` object |

Instance and static synchronized methods therefore do **not** exclude one another.
Different instances also do not coordinate unless they deliberately share a lock.

### Prefer a private lock object

```java
final class Inventory {
    private final Object lock = new Object();
    private int available;

    Inventory(int initialStock) {
        this.available = initialStock;
    }

    public boolean reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        synchronized (lock) {
            if (available < quantity) {
                return false;
            }
            available -= quantity; // linearization point
            return true;
        }
    }

    public int available() {
        synchronized (lock) {
            return available;
        }
    }
}
```

The invariant is `available >= 0`. Checking stock and decrementing it must be one atomic critical section.

### Interview rules

- Lock around the invariant, not merely around individual fields.
- Keep critical sections small.
- Do not perform slow I/O, remote calls, callbacks, or arbitrary user code while holding a lock.
- Prefer a private final lock; do not lock on public objects, string literals, or externally supplied instances.
- Document the lock guarding each field when the design is nontrivial.

## 6. `wait`, `notify`, and `notifyAll`

These methods coordinate threads using an intrinsic monitor.

- The caller must own the monitor.
- `wait()` atomically releases the monitor and suspends the thread.
- Before returning, `wait()` reacquires the monitor.
- Always test the condition in a `while` loop because of spurious wake-ups and competition.
- Prefer `notifyAll()` unless you can prove one awakened waiter category is sufficient.

```java
synchronized (lock) {
    while (!conditionIsTrue()) {
        lock.wait();
    }
    changeProtectedState();
}
```

For most interview code, prefer `BlockingQueue`, `CountDownLatch`, or `Condition` because they express intent more clearly.

## 7. `Lock`, `ReentrantLock`, and `Condition`

Use `ReentrantLock` when you specifically need:

- interruptible acquisition with `lockInterruptibly()`;
- timed/non-blocking acquisition with `tryLock()`;
- more than one condition wait-set;
- optional fairness;
- explicit lock operations across a structure that cannot use a lexical `synchronized` block.

Always unlock in `finally`:

```java
lock.lock();
try {
    updateProtectedState();
} finally {
    lock.unlock();
}
```

A `Condition` is the explicit-lock equivalent of `wait/notify`. Each condition represents a predicate such as `notEmpty` or `notFull`.

```java
final class BoundedBuffer<T> {
    private final Queue<T> queue = new ArrayDeque<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    BoundedBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    void put(T item) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            T item = queue.remove();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

In production, use a standard `BlockingQueue`; this implementation is useful for demonstrating the protocol in an interview.

### Read-write locks

`ReentrantReadWriteLock` permits multiple readers or one writer. Consider it only when:

- reads greatly outnumber writes;
- read sections are meaningful enough to offset coordination overhead;
- the invariant can safely distinguish read and write access.

A normal lock is often faster and safer for small critical sections. Mention `StampedLock` only as an advanced optimization requiring careful validation; its optimistic reads are more error-prone and it is not reentrant.

Do not casually upgrade a read lock to a write lock. With
`ReentrantReadWriteLock`, two readers can both wait forever while retaining read
ownership and trying to acquire the write lock. Release the read lock and retry under
the write lock, then recheck the predicate.

## 8. ExecutorService and thread pools

### Why use an executor?

An executor separates **what work should run** from **how workers are managed**. It gives you:

- worker reuse;
- bounded concurrency;
- task queues;
- lifecycle and shutdown;
- futures and exception propagation;
- configurable rejection/backpressure.

### Know the ThreadPoolExecutor parameters

```java
ExecutorService executor = new ThreadPoolExecutor(
        4,                              // corePoolSize
        8,                              // maximumPoolSize
        30, TimeUnit.SECONDS,           // idle timeout above core size
        new ArrayBlockingQueue<>(100),  // bounded waiting work
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

Explain each decision:

- **Core pool size:** workers normally kept available.
- **Maximum pool size:** upper bound when the queue cannot accept more work.
- **Keep-alive:** retirement policy for extra workers.
- **Work queue:** admission buffering and ordering.
- **Thread factory:** naming, daemon policy, uncaught-exception behavior.
- **Rejection policy:** what happens under saturation.

Name workers with an injected `ThreadFactory`; thread names turn production dumps and
metrics into evidence instead of guesswork. The component that creates an executor
owns its lifecycle. If a caller injects an executor, document whether ownership stays
with the caller rather than shutting down someone else's pool.

### Queue choice changes pool behavior

- Unbounded queue: tasks accumulate; `maximumPoolSize` may become ineffective; memory and latency can grow without limit.
- Bounded queue: makes capacity explicit and enables backpressure/rejection.
- `SynchronousQueue`: no storage; task submission hands directly to a worker.
- Priority queue: requires a meaningful ordering and starvation analysis.

### Rejection policies

- `AbortPolicy`: fail fast with `RejectedExecutionException`.
- `CallerRunsPolicy`: submitting thread executes the task, naturally slowing producers.
- `DiscardPolicy`: silently drops; rarely safe.
- `DiscardOldestPolicy`: drops queued work; safe only with an explicit business rule.
- Custom policy: record metrics, block with a timeout, or route to durable storage.

Never say “we will use an unbounded queue” without explaining memory and latency consequences.

### `execute` versus `submit`

| API | Result and failure behavior | Use when |
| --- | --- | --- |
| `execute(Runnable)` | No `Future`; an uncaught task exception reaches the worker's uncaught-exception path. | Fire-and-observe work with an explicit failure reporter. |
| `submit(...)` | Returns a `Future`; the exception is captured and rethrown by `get()` as `ExecutionException`. | The caller needs a result, cancellation, completion, or deterministic failure propagation. |

If nobody observes a submitted future, a failed task can look successful. For batches,
`invokeAll` returns futures in input order; `ExecutorCompletionService` exposes them in
completion order. An uncaught exception in a periodic scheduled task suppresses later
executions, so periodic work must record failure deliberately or have its future
observed.

### Correct shutdown

```java
executor.shutdown(); // stop accepting, finish accepted tasks
try {
    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // interrupt running tasks, return queued tasks
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate");
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

Define these interview behaviors:

- Are new tasks rejected after shutdown?
- Do accepted tasks drain?
- Are running tasks interruptible?
- What happens to queued tasks during immediate shutdown?
- How does the caller learn that termination completed?

Thread-pool threads are reused. Clear request-scoped `ThreadLocal` state in `finally`
or, preferably, pass context explicitly; otherwise one request can leak identity,
tracing, or transaction state into the next task.

### Pool sizing

- CPU-bound work: begin near the number of available processors.
- I/O-bound work: more concurrency may help because tasks spend time waiting.
- Measure queue time, task time, utilization, rejection rate, and tail latency.
- Separate pools when slow/blocking tasks could starve latency-sensitive work.

At minimum expose active workers, queue depth and remaining capacity, queue wait,
execution latency, rejection count, task failures, timeouts, and shutdown drain time.
These signals distinguish an undersized pool from a slow dependency or an overloaded
admission policy.

Do not present a sizing formula as universally correct. Workload measurement and resource limits decide the final number.

### Common executor mistakes

- Creating a new pool per request.
- Never shutting down the pool.
- Submitting a task to a pool and blocking on another task submitted to the same saturated pool: thread-starvation deadlock.
- Ignoring exceptions because `submit()` captures them inside `Future`.
- Using `Executors.newFixedThreadPool()` without noticing its unbounded queue.
- Holding a domain lock while waiting on `Future.get()`.

## 9. BlockingQueue and producer-consumer

A `BlockingQueue` combines a thread-safe queue with condition waiting.

| Operation | Failure behavior |
| --- | --- |
| `add` / `remove` | Throws if it cannot complete immediately. |
| `offer` / `poll` | Returns a status or `null` immediately. |
| timed `offer` / `poll` | Waits up to a timeout. |
| `put` / `take` | Waits until it can complete. |

### Implementations to know

- `ArrayBlockingQueue`: bounded array; predictable capacity; optional fairness.
- `LinkedBlockingQueue`: optionally bounded; default capacity is extremely large, so specify one.
- `PriorityBlockingQueue`: unbounded priority ordering; no FIFO guarantee for equal priority unless you add one.
- `DelayQueue`: items become available after a delay.
- `SynchronousQueue`: direct handoff with zero storage.
- `LinkedTransferQueue`: supports immediate transfer to a waiting consumer.

Queue capacity is a product/API decision: a full queue may block, time out, reject,
shed lower-priority work, or persist elsewhere. `size()` is an observation—not a
check-then-act synchronization mechanism—and can be stale as soon as it returns.

### Producer-consumer example

```java
record Job(String id) {}

final class JobProcessor implements AutoCloseable {
    private static final Job POISON = new Job("__STOP__");
    private final BlockingQueue<Job> queue = new ArrayBlockingQueue<>(100);
    private final ExecutorService consumers;
    private final int consumerCount;

    JobProcessor(int consumerCount) {
        this.consumerCount = consumerCount;
        this.consumers = Executors.newFixedThreadPool(consumerCount);
        for (int i = 0; i < consumerCount; i++) {
            consumers.submit(this::consume);
        }
    }

    boolean submit(Job job, Duration timeout) throws InterruptedException {
        return queue.offer(job, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void consume() {
        try {
            while (true) {
                Job job = queue.take();
                if (job == POISON) return;
                process(job);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void process(Job job) {
        // Perform work; catch/report per-job failures as required.
    }

    @Override
    public void close() throws InterruptedException {
        for (int i = 0; i < consumerCount; i++) {
            queue.put(POISON);
        }
        consumers.shutdown();
        consumers.awaitTermination(30, TimeUnit.SECONDS);
    }
}
```

Interview discussion:

- A bounded queue prevents unlimited memory growth.
- Timed `offer` defines backpressure at submission.
- One poison item is needed per consumer in this particular shutdown protocol.
- Identity comparison is intentional because the poison object is private.
- A real service must define what happens when processing fails or close is interrupted.

## 10. Semaphore

A semaphore controls a number of permits. It is useful for limiting concurrent access to a scarce resource, even when different threads may acquire and release permits.

```java
final class LimitedClient {
    private final Semaphore permits = new Semaphore(10, true);

    Response call(Request request) throws InterruptedException {
        if (!permits.tryAcquire(200, TimeUnit.MILLISECONDS)) {
            throw new ServiceBusyException();
        }
        try {
            return performRemoteCall(request);
        } finally {
            permits.release();
        }
    }
}
```

Use cases:

- connection-pool capacity;
- limiting concurrent downloads;
- bounding calls to a fragile dependency;
- parking spots or shared devices in an LLD model.

Pitfalls:

- Releasing without acquiring creates permit leaks in the opposite direction.
- Release in `finally`, but only after acquisition succeeded; forgetting `finally`
  permanently loses permits.
- A semaphore limits concurrency; it does not protect a multi-field invariant unless used as a one-permit mutex with a correct protocol.
- A semaphore has no thread-ownership rule: a different thread can release a permit.
  A binary semaphore is therefore not automatically a better mutex.
- Fairness can reduce starvation but may reduce throughput, and it does not guarantee
  end-to-end fairness once queues, retries, and external dependencies are involved.

## 11. Coordination utilities

### CountDownLatch

One-shot gate. One or more threads wait until a count reaches zero.

Use for “wait until N startup tasks finish.” It cannot be reset.

### CyclicBarrier

A fixed number of participants meet at a barrier before beginning the next phase. It can be reused, but a failed/interrupted participant can break the barrier.

### Phaser

Multi-phase coordination with a participant count that can change. Useful for staged workflows, but more complex than a latch or barrier.

### Exchanger

Two threads exchange values at a rendezvous point. Rare in ordinary LLD interviews but useful to recognize.

### CompletableFuture

Use for asynchronous composition, not as an excuse to make every method asynchronous.

```java
CompletableFuture<User> user = CompletableFuture.supplyAsync(
        () -> userClient.load(userId), ioExecutor);
CompletableFuture<List<Order>> orders = CompletableFuture.supplyAsync(
        () -> orderClient.forUser(userId), ioExecutor);

CompletableFuture<UserSummary> summary = user.thenCombine(
        orders, UserSummary::new);
```

Explain:

- which executor runs each stage;
- how exceptions are recovered or propagated;
- timeouts and cancellation;
- whether downstream work should continue after one branch fails;
- why blocking `join()` is safe at the chosen boundary.

`thenApply` transforms a value; `thenCompose` flattens a dependent asynchronous
operation. `allOf` waits for completion but does not collect typed results or define
partial-failure behavior. Pass an explicit, owned executor for blocking work rather
than silently consuming the common fork/join pool. Do not call `join()` while holding
a lock needed by a stage. Cancelling a `CompletableFuture` does not prove that its
underlying remote operation stopped or rolled back.

## 12. Concurrent collections and atomics

### ConcurrentHashMap

Prefer atomic map operations for compound actions:

```java
sessions.computeIfAbsent(userId, id -> createSession(id));
counters.merge(key, 1L, Long::sum);
```

Do not write `containsKey` followed by `put`; another thread can interleave.

Remember that thread-safe individual map operations do not automatically make a transaction across multiple keys atomic.
Mutable values stored in a concurrent map do not become transitively thread-safe.

### CopyOnWriteArrayList

Good when reads/iteration are frequent, writes are rare, and snapshot iteration is useful. Every write copies the backing array, so it is poor for write-heavy or large collections.

### ConcurrentLinkedQueue

Non-blocking unbounded queue. It does not provide backpressure or waiting semantics.

### Atomic classes

- `AtomicInteger`, `AtomicLong`, `AtomicBoolean`, `AtomicReference`
- `LongAdder` for high-contention statistics where an exact instantaneous total is not required for a decision
- atomic `compareAndSet` for conditional state transitions

```java
enum State { NEW, RUNNING, COMPLETED, CANCELLED }

private final AtomicReference<State> state =
        new AtomicReference<>(State.NEW);

boolean start() {
    return state.compareAndSet(State.NEW, State.RUNNING);
}
```

Atomics are best for simple independent state. If correctness spans several fields, a lock or immutable state object is usually clearer.
Functions supplied to CAS/update loops may execute more than once, so keep them free
of logging, network calls, and other externally visible side effects. `LongAdder` is
excellent for metrics but not for exact inventory or quota decisions.

### ABA and lock-free warning

Compare-and-set can succeed even if a value changed from A to B and back to A. Versioned references can help, but implementing lock-free data structures is an advanced exercise. In most LLD interviews, choose standard concurrent collections rather than inventing one.

## 13. Interruption, cancellation, and timeouts

Interruption is a cooperative cancellation request, not forced thread termination.

Rules:

- A blocking method may throw `InterruptedException` and clear the interrupt flag.
- If you cannot propagate the exception, usually restore the status with `Thread.currentThread().interrupt()`.
- Long-running loops should periodically block interruptibly or check `isInterrupted()`.
- Never swallow interruption silently.
- `Future.cancel(true)` requests interruption; it does not guarantee that a task stops.
- Do not use deprecated forced-stop APIs.
- Use `System.nanoTime()` for elapsed deadlines and TTL calculations; wall-clock time
  may jump.
- A remote timeout is normally an **unknown business outcome**, not proof that the
  provider performed no side effect. Reconciliation/idempotency is separate from
  thread cancellation.

```java
try {
    queue.put(item);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

Every blocking operation in your design should have an answer for timeout, interruption, and shutdown.

## 14. Failure modes

### Race condition

Correctness depends on the timing/interleaving of operations. Fix the full invariant, not only the visible symptom.

### Deadlock

Deadlock requires the four Coffman conditions: mutual exclusion, hold-and-wait, no
preemption, and circular wait. Break at least one—most interview designs break circular
wait with a stable global lock order and reduce hold-and-wait. Prevent with:

- global lock ordering;
- one lock where practical;
- small lock scope;
- no external calls while holding locks;
- avoiding hold-and-wait;
- timed acquisition as failure containment, not as a substitute for a correct protocol.

```java
Lock first = accountA.id().compareTo(accountB.id()) < 0
        ? accountA.lock() : accountB.lock();
Lock second = first == accountA.lock()
        ? accountB.lock() : accountA.lock();
```

Every transfer uses the same stable ordering.

### Livelock

Threads keep reacting to one another but make no progress. Use deterministic ownership, randomized backoff, or bounded retries.

### Starvation

A participant repeatedly fails to obtain CPU, a lock, a permit, or queue capacity. Consider fairness, bounded work, and separate resource pools.

### Lost notification

A state change and notification do not follow the same predicate/lock protocol. Wait in a loop and update the state while holding the associated lock.

### Thread starvation deadlock

All pool workers wait for tasks queued to the same pool, so the required tasks never run. Avoid blocking dependencies within a saturated pool or separate the workloads.

## 15. Primitive selection guide

| Need | Prefer | Why |
| --- | --- | --- |
| Prevent shared mutation | Immutability or thread confinement | Removes coordination entirely. |
| Protect a short invariant | `synchronized` | Smallest, safest general lock. |
| Timed/interruptible locking or multiple conditions | `ReentrantLock` | Explicit advanced lock capabilities. |
| Publish one state flag/reference | `volatile` | Visibility without compound atomicity. |
| Atomic counter/state transition | Atomic class | Clear lock-free single-value update. |
| Submit/manage asynchronous work | `ExecutorService` | Separates tasks from worker lifecycle. |
| Bounded producer-consumer handoff | `BlockingQueue` | Queue + waiting + backpressure. |
| Limit concurrent use of a resource | `Semaphore` | Represents N available permits. |
| Wait for N completions once | `CountDownLatch` | One-shot countdown gate. |
| Repeated fixed-party phase | `CyclicBarrier` | Reusable rendezvous. |
| Dynamic multi-phase workflow | `Phaser` | Flexible parties and phases. |
| Thread-safe keyed access | `ConcurrentHashMap` | Atomic per-key operations. |
| Mostly-read listener snapshot | `CopyOnWriteArrayList` | Lock-free reads and snapshot iteration. |
| Compose independent async results | `CompletableFuture` | Declarative completion graph. |

## 16. Applying concurrency to LLD problems

Classify the pressure before selecting a primitive:

- **Correctness:** two callers must not spend the same inventory, quota, seat, or
  state transition.
- **Coordination:** producers, consumers, timers, and stages must hand work off and
  terminate predictably.
- **Scarcity:** only N callers may use a connection, provider, dock, worker, or other
  limited resource concurrently.

The same design can have all three pressures; do not ask one primitive to solve them
all.

| Featured interview problem | Protected invariant and Java boundary | Staff-level follow-up |
| --- | --- | --- |
| Order Management System | Serialize valid transitions per order with a private lock/version; publish events only after leaving the aggregate lock. | Durable optimistic version, transactional outbox, idempotent consumers, stuck-order reconciliation. |
| Inventory Reservation Service | Atomic check-and-reserve per SKU; acquire multiple SKU locks in sorted order and make release/expiry idempotent. | Database conditional update/OCC, reservation lease, fencing/version, partition hot SKUs. |
| Coupon / Promotion Engine | Publish immutable rule snapshots; atomically consume per-customer/global usage caps. | Versioned rules, durable quota ledger or atomic shared-store script, audit/replay. |
| Notification Framework | Bounded `BlockingQueue` + owned executor; semaphore bulkhead per provider; define per-recipient ordering and dedupe. | Durable broker, retry/dead-letter policy, partition ordering, provider-specific circuit breaking. |
| Payment Processing Service | Idempotent payment state machine; do not hold a lock over gateway I/O; cap calls with a semaphore and model timeout as unknown outcome. | Durable workflow, callback dedupe, provider reconciliation, fencing/versioned transitions. |
| Warehouse Fulfilment Domain | Conditional task claim; queues coordinate workers; semaphores cap docks/equipment; sorted locks for multi-bin moves. | Durable leases with fencing, work partitioning, rebalancing, abandoned-claim recovery. |
| Rate Limiter | Atomic per-key token/window transition using a per-key or striped lock and monotonic time; clean stale keys safely. | Atomic Redis/scripted transition, hot-key partitioning, clock/replication trade-offs. |
| Product Catalog Service | Publish immutable catalog/index snapshots through a volatile/atomic reference; serialize versioned writes and reindex asynchronously. | Database OCC, change stream, versioned search index, rebuild and consistency lag observability. |

Additional practice mappings:

- **Parking lot:** a semaphore can represent total capacity, while spot assignment
  still needs one atomic allocation boundary.
- **Elevator:** use a thread-safe command queue and single-threaded ownership of each
  car's state; keep dispatch policy separate.
- **Logging:** bound the asynchronous buffer, select block/drop/spill behavior, and
  define flush guarantees on shutdown.
- **Cache:** coordinate storage and eviction together, coalesce duplicate loads, and
  define expiration/refresh ownership; a concurrent map alone is insufficient.
- **Job scheduler/thread pool:** pair due-time ordering with bounded workers and an
  explicit task lifecycle, rejection, cancellation, retry, and shutdown contract.

## 17. A 60-minute LLD concurrency approach

### Minutes 0–8: clarify

Ask:

- Which commands may happen concurrently?
- Is state process-local or shared across services?
- What consistency guarantee is required?
- What capacity limits and latency expectations exist?
- What should happen under overload, timeout, cancellation, and shutdown?

### Minutes 8–15: state invariants

Examples:

- A seat cannot have two active bookings.
- Inventory never becomes negative.
- A task is accepted at most once and reaches one terminal state.
- A worker never executes user code while holding the pool-management lock.

### Minutes 15–30: model

Separate:

- domain state;
- concurrency boundary;
- queue/resource policy;
- lifecycle state machine;
- external dependencies.

Prefer one owner per mutable aggregate where possible.

### Minutes 30–45: critical flows

Walk through:

- two callers competing for the last unit;
- queue full;
- worker failure;
- cancellation while queued and while running;
- graceful versus immediate shutdown;
- timeout and interruption.

### Minutes 45–55: failure analysis

State lock order, waiting predicates, backpressure, starvation risk, and observability.

### Minutes 55–60: testing and trade-offs

Explain deterministic synchronization tests, stress tests, metrics, and how the design could scale beyond one JVM.

## 18. Testing concurrent code

Avoid tests that only “sleep and hope.” Use coordination primitives to force interleavings.

### Essential techniques

- Use `CountDownLatch` to release competing threads at once.
- Use another latch to know all threads finished.
- Retain futures and call `get()` so task failures are asserted on the test thread.
- Add timeouts so deadlocks fail rather than hang the suite.
- Repeat race-sensitive tests many times as a supplement, not the only proof.
- Assert invariants, not a particular scheduling order unless ordering is the contract.
- Exercise full/empty queues, rejection, interruption, cancellation, shutdown,
  duplicate commands, and double completion—not only the happy race.

### Example test shape

```java
int contenders = 20;
ExecutorService pool = Executors.newFixedThreadPool(contenders);
CountDownLatch ready = new CountDownLatch(contenders);
CountDownLatch start = new CountDownLatch(1);
CountDownLatch done = new CountDownLatch(contenders);
AtomicInteger winners = new AtomicInteger();

for (int i = 0; i < contenders; i++) {
    pool.submit(() -> {
        ready.countDown();
        try {
            start.await();
            if (inventory.reserve(1)) winners.incrementAndGet();
        } finally {
            done.countDown();
        }
        return null;
    });
}

assertTrue(ready.await(2, TimeUnit.SECONDS));
start.countDown();
assertTrue(done.await(2, TimeUnit.SECONDS));
assertEquals(1, winners.get());
assertEquals(0, inventory.available());
pool.shutdownNow();
```

Useful specialist tools include JMH for benchmarks and JCStress for Java Memory Model/concurrency stress testing, but interview explanations should begin with invariant-based tests.

## 19. Senior and Staff answer bar

| Area | Strong Senior answer | Staff-level extension |
| --- | --- | --- |
| Correctness | Names the invariant, guard, and linearization point. | Separates JVM safety, durable truth, and cross-node ownership. |
| Primitive choice | Uses the smallest fitting JDK abstraction and handles exceptions. | Defines contention, fairness, overload, hot-key, and migration trade-offs. |
| Lifecycle | Covers timeout, interruption, rejection, and shutdown. | Adds draining, replay, repair, rollout, and operator controls. |
| Testing | Uses gates/latches, bounded waits, observed futures, and invariant assertions. | Adds stress/model checking plus production saturation and liveness signals. |
| Scale | Avoids obviously harmful global serialization. | Budgets workers/queues, isolates workloads, and evolves the linearization point. |

### Evolving beyond one JVM

| Local mechanism | What it guarantees | Typical distributed evolution |
| --- | --- | --- |
| Aggregate lock / atomic reference | One-process mutual exclusion or CAS | Database optimistic version, conditional update, row lock, or single partition owner |
| `BlockingQueue` | In-memory handoff and backpressure | Durable broker/stream with explicit acknowledgement, replay, and dead-letter policy |
| `Semaphore` | One-process admission cap | Shared quota/admission service, provider limit at every caller, or broker consumer concurrency |
| `Future` / in-memory workflow | One-process completion signal | Durable workflow/state machine plus idempotency and reconciliation |

### Follow-ups you should answer directly

- What is the exact linearization point?
- What does the producer observe when capacity is full?
- What if a waiter is interrupted or the component shuts down?
- Can callbacks re-enter this component, and do any run under a lock?
- How are multiple keys ordered, and what happens under a hot key?
- What happens when a task throws or a remote timeout has an unknown outcome?
- Which metrics distinguish queueing, starvation, permit exhaustion, and a slow
  dependency?
- What replaces this lock, queue, semaphore, or future across ten instances?

### Rapid interview questions and expected answers

| Question | Expected core answer |
| --- | --- |
| Why not create a thread per task? | Thread creation and stacks are costly; an executor owns bounded concurrency, admission, failure observation, and shutdown. |
| Why can `newFixedThreadPool` still overload? | Its worker count is fixed but its default queue is unbounded, so memory and queue latency can grow. |
| `execute` or `submit`? | Use `execute` with an explicit uncaught-failure path; use `submit` when a `Future` will carry result, cancellation, or failure to an observer. |
| What happens when the executor queue is full? | Apply a declared product policy: reject, caller-runs, timed admission, shed, or persist; always emit saturation metrics. |
| Why a bounded `BlockingQueue`? | It turns finite downstream capacity into explicit backpressure rather than hidden memory growth. |
| How do consumers stop? | Choose interruption, a close flag/protocol, or one poison item per consumer; define producer races and whether accepted work drains. |
| Semaphore or mutex? | A semaphore limits N admissions and has no owner; a mutex protects an invariant with ownership. They solve different problems. |
| `synchronized` or `ReentrantLock`? | Start with `synchronized`; choose explicit lock only for timed/interruptible acquisition, fairness, or multiple conditions. |
| Does `volatile` make `count++` safe? | No. It provides visibility/ordering for the variable, not atomic read-modify-write. |
| Why wait in a `while` loop? | Spurious wake-ups occur and another waiter may consume the condition before this thread reacquires the lock. |
| Is `ConcurrentHashMap` enough for reservation? | Only when one atomic map operation spans the invariant; check-then-act or multi-key all-or-none work needs a wider boundary. |
| What should catch `InterruptedException` do? | Propagate it when possible; otherwise restore the interrupt flag and move to a safe cancellation boundary. |
| How do you prove a race fix? | Force contention with gates/latches, observe futures with bounded waits, then assert the domain invariant and terminal lifecycle. |
| Does a JVM lock protect ten replicas? | No. Move the linearization point to a database conditional update/transaction, atomic shared-store operation, durable owner, or fenced lease. |

In a one-hour interview, implement the behavior-rich aggregate/coordinator, bounded
admission, exception-safe cleanup, shutdown path, and one deterministic race test.
Explain rather than type HTTP/ORM/broker boilerplate, distributed-lock internals,
complete retry infrastructure, dashboards, and deployment configuration.

## 20. Modern Java: virtual threads

On Java versions that support virtual threads, they can make thread-per-request code practical for large numbers of mostly blocking tasks:

```java
try (ExecutorService executor =
         Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Response> response = executor.submit(() -> blockingCall());
}
```

Know the interview boundaries:

- Virtual threads improve scalability for blocking workloads, not CPU execution speed.
- They do not remove races or the need for synchronization.
- Continue to limit scarce resources with semaphores, pools, or backpressure.
- Avoid treating the executor as the resource limit; bound the actual dependency.
- Confirm the Java version allowed by the interview or codebase. The maintained examples in this repository target Java 17 unless a problem says otherwise.

## 21. Topics you should be able to answer

### Must know

- Thread versus process; concurrency versus parallelism
- Race condition, atomicity, visibility, ordering
- Java Memory Model and happens-before basics
- `synchronized`, monitor ownership, reentrancy
- `volatile` and why `count++` still races
- `wait/notifyAll` and condition loops
- `ExecutorService`, `ThreadPoolExecutor`, task queues, rejection
- Graceful and immediate shutdown
- `BlockingQueue` and bounded producer-consumer
- `Semaphore`
- `CountDownLatch` and `CyclicBarrier`
- `ConcurrentHashMap` compound operations
- Atomics and compare-and-set
- Interruption and cooperative cancellation
- Deadlock, livelock, starvation, and lost notification

### Good to know

- `ReentrantLock`, `Condition`, `ReadWriteLock`
- `CompletableFuture`
- `CopyOnWriteArrayList`, concurrent queues
- `LongAdder`
- Lock ordering and lock striping
- Thread factories and uncaught exceptions
- Virtual threads

### Advanced / optional

- `StampedLock`
- `Phaser`, `Exchanger`, `TransferQueue`
- Fork/join and work stealing
- False sharing and cache contention
- ABA and versioned atomics
- Lock-free algorithm proofs
- JCStress and JMH

## 22. Practice sequence

Complete these in order:

1. Thread-safe counter: `synchronized`, `AtomicInteger`, `LongAdder`; compare semantics.
2. Bounded blocking queue using `ReentrantLock` and two conditions.
3. Producer-consumer using the standard `BlockingQueue`.
4. Print odd/even numbers using a condition.
5. Multithreaded FizzBuzz.
6. Resource pool using `Semaphore`.
7. TTL cache with `ConcurrentHashMap` and defined cleanup ownership.
8. Rate limiter with atomic per-key state.
9. Custom thread pool with rejection, future state, and shutdown.
10. Inventory reservation with competing requests and multi-key lock ordering.

For each exercise, write:

- protected invariant;
- owner/guard of each mutable field;
- linearization point;
- lock order;
- waiting predicate;
- overload behavior;
- interruption and shutdown behavior;
- deterministic concurrency test.

## 23. Final readiness checklist

You are ready when you can answer “yes” to these questions:

- Can I explain why a concrete interleaving breaks unsynchronized code?
- Can I distinguish visibility from atomicity?
- Can I explain when `volatile` is sufficient and when it is not?
- Can I write correct `synchronized` and `ReentrantLock` code?
- Can I implement a condition wait with a `while` loop?
- Can I configure a bounded `ThreadPoolExecutor` and justify every parameter?
- Can I explain backpressure and choose a rejection policy?
- Can I design orderly and immediate shutdown?
- Can I choose the right `BlockingQueue`?
- Can I distinguish a mutex, semaphore, latch, and barrier?
- Can I preserve interruption correctly?
- Can I identify deadlock, livelock, starvation, and pool-starvation deadlock?
- Can I use atomic `ConcurrentHashMap` operations instead of check-then-act?
- Can I state the invariant and linearization point in an LLD problem?
- Can I test a race without depending only on sleeps?

## 24. Runnable Java 17 lab and source integration

The [interview lab](interview/README.md) provides one deterministic runnable program
for the four requested core topics:

1. `ExecutorService` lifecycle and `Future` failure propagation;
2. bounded `BlockingQueue` backpressure and producer-consumer handoff;
3. exception-safe `Semaphore` admission;
4. compound invariants using both `synchronized` and `ReentrantLock`.

This guide consolidates the repository's [fundamentals](README.md), [monitor and
thread introduction](Multithreading/Introduction.md), [question bank](questions/README.md),
and cloned source material. The pinned inputs are:

- [`awesome-low-level-design` at `fc26e40`](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src)
  (GPL-3.0 Java examples);
- [`low-level-design-primer` at `49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md)
  (requirements/prompts; no repository-wide license found at that revision);
- [`kumaransg/LLD` at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems)
  (unlicensed fragments summarized as code-review inputs rather than copied).

Some older local/clone fragments intentionally remain review exercises: they
busy-spin, depend on sleeps, omit shutdown, swallow interruption, or can leak
permits. Use the Java 17 lab as the corrected executable baseline and the warnings in
the [concurrency index](README.md) when reviewing those examples.

## Related notes

- [Concurrency index](README.md)
- [Java multithreading introduction](Multithreading/Introduction.md)
- [Concurrency question bank](questions/README.md)
- [Runnable Java 17 interview lab](interview/README.md)
- [Custom thread-pool executor](../Problems/thread_pool_executor/README.md)
