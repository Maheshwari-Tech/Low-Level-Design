# Amazon/Coupang LLD Reference Solutions — Python 3.9+

This package mirrors the seven featured Java interview problems in runnable, standard-library-only
Python. It keeps the domain concepts aligned while using idiomatic Python rather than mechanically
translating Java classes.

## Run everything

From this directory:

```bash
python3 -m amazon_coupang_lld.run_all
```

Every module contains an assertion-backed scenario and may also be imported independently.

## Problem map

| Module | Main design challenge | Python knowledge demonstrated |
|---|---|---|
| `order_management.py` | Idempotent checkout saga and compensation | `asyncio.Task`, `Lock`, `Protocol`, frozen command/value dataclasses, async Observer |
| `inventory_reservation.py` | All-or-none reservation and no oversell | `RLock`, `ThreadPoolExecutor`, guarded mutable aggregate, TTL expiry method |
| `promotion_engine.py` | Composable rules/benefits and atomic redemption | `Protocol`, immutable strategies, deterministic sorting, locked redemption ledger |
| `notification_framework.py` | Bounded producer-consumer delivery | `asyncio.Queue`, worker tasks, retry, backpressure, callbacks/events, dedupe futures |
| `payment_processing.py` | Unknown provider outcome and duplicate callbacks | async idempotent commands, append-only ledger, per-payment `RLock`, retry and webhook dedupe |
| `warehouse_fulfilment.py` | Task claim and legal fulfillment transitions | `Queue`, task-level `Lock`, strategy protocol, async carrier port |
| `rate_limiter.py` | Atomic refill/check/consume per key | map lock plus per-bucket lock, injected monotonic clock, threaded contention test |

Shared utilities live in `support.py`:

- `Money` is an immutable value object using integral minor units and explicit currency.
- `RetryPolicy` and `async_retry` implement capped exponential retry with `asyncio.sleep`, which
  suspends the coroutine rather than blocking the event-loop thread.
- `EventBus` is a local async Observer that supports synchronous or asynchronous handlers.

## Python-specific interview points

### The GIL is not a correctness strategy

The GIL does not make a multi-step invariant atomic. Thread switches can occur between bytecode
operations, C extensions may release the GIL, and multiple processes/instances have no shared GIL.
Use a lock for the in-memory model and explain the production transaction, conditional write, or
atomic Redis script.

### `asyncio` is concurrency, not automatic parallelism

`asyncio` is ideal for payment, carrier, and notification I/O. A coroutine must not call blocking
SDK/database code directly; use a native async client or move unavoidable blocking work to
`asyncio.to_thread`. CPU-heavy work belongs in a process pool or external worker.

### Preserve one idempotent task

Order, notification, and payment store the first `Task`/`Future` by idempotency key. Duplicate
callers await the same outcome. `asyncio.shield` prevents one caller's cancellation from cancelling
the shared operation for every other caller. Production stores a request fingerprint and durable
result, rejecting reuse of the same key with different parameters.

### Keep sync locks out of awaited regions

The examples never hold `threading.Lock`/`RLock` across `await`. Domain mutations are short and
synchronous; slow external I/O happens after the lock is released. Holding a thread lock while
suspending a coroutine can deadlock the event loop and destroys concurrency.

### Bound queues and in-flight work

`NotificationService` uses bounded `asyncio.Queue`; `put_nowait` makes overload visible. Its worker
count bounds in-flight provider calls. Production must decide whether a full queue blocks with a
deadline, rejects, sheds priority, or writes to a durable broker.

### Cancellation is part of the contract

`CancelledError` inherits from `BaseException` in current Python. Retry classification must not
retry cancellation. Shared tasks are shielded for duplicate callers, while service shutdown should
explicitly drain or cancel owned workers and surface incomplete work.

## Patterns and principles

- **Aggregate/state machine:** order, payment, and fulfillment own legal transitions.
- **Strategy:** promotion conditions/benefits, warehouse allocation, and rate policy resolution.
- **Adapter/port:** provider, payment, inventory, and carrier `Protocol` definitions isolate SDKs.
- **Observer:** `EventBus` handles local reactions; a production outbox provides durability.
- **Producer-consumer:** notification queue decouples admission from provider latency.
- **Dependency inversion:** workflows depend on `Protocol` ports instead of concrete clients.
- **Immutability:** commands, money, events, and snapshots use frozen dataclasses.
- **Composition over inheritance:** promotion behavior is assembled from condition and benefit
  objects rather than subclass combinations.

## Production substitutions

These are interview-sized in-memory implementations. For production:

- replace dictionaries with repositories and explicit Unit of Work;
- persist idempotency key, request hash, outcome, and retention/expiry;
- commit aggregate and outbox event atomically;
- consume at least once and deduplicate by event/command ID;
- use database constraints/version checks for money and inventory invariants;
- add jitter, total deadlines, metrics, tracing, circuit breaking, and reconciliation;
- validate callback authenticity and protect tokens/PII.

The detailed requirements, variations, scoring rubrics, and production trade-offs remain in the
corresponding Java problem README files under `Problems/`; the invariants are language-independent.
