# Amazon / Coupang LLD Strong-Hire Guide

This is the navigation and execution guide for the seven requested one-hour problems. The original
request says eight but names seven. The root catalog already has an eighth featured problem,
[Product Catalog Service](../Problems/product_catalog_service/), if an additional mock is needed.

## What the companies signal

Amazon's official software-development topics include object-oriented design, databases, and
distributed computing. Its Senior SDE guidance names practicality, accuracy, efficiency,
reliability, optimization, and scalability as design objectives and says interview code should be
syntactically correct rather than pseudocode.

Coupang's interview guidance says problems may be intentionally vague. Candidates are expected to
clarify requirements and identify the constraints that matter technically; its stated areas include
APIs, object-oriented design/programming, data structures, and system design.

Primary sources:

- [Amazon software-development interview topics](https://amazon.jobs/content/en/how-we-hire/interview-prep/software-development-topics)
- [Amazon SDE III interview preparation](https://amazon.jobs/content/en/how-we-hire/sde-iii-interview-prep)
- [Coupang interview tips](https://www.coupang.jobs/en/interview-tips/)

## The 60-minute answer

| Time | Deliverable | Strong-hire signal |
|---:|---|---|
| 0–5 | Actors, scope, scale/concurrency, consistency, non-goals | Handles ambiguity deliberately |
| 5–10 | Happy path plus 2–4 invariants | Finds the real correctness boundary |
| 10–20 | Entities, ownership, states, ports, one sequence | Cohesive responsibilities |
| 20–40 | Compilable aggregate and main application flow | Strong Java and executable thinking |
| 40–50 | Race/failure handling and focused tests | Reliability rather than happy-path OOP |
| 50–57 | Two requirement variations | Extensibility justified by actual change |
| 57–60 | Trade-offs and production substitution | Good judgment and clear closure |

Suggested opening:

> I will define the invariant and consistency boundary first, implement the main in-process domain
> flow, and keep external providers behind asynchronous, idempotent ports. Then I will test one
> race and show how two likely variations extend the design.

## Problem map

| Problem | Core invariant/challenge | Patterns that earn their place | Java focus |
|---|---|---|---|
| [Order Management](../Problems/order_processing_system/) | Legal lifecycle plus inventory/payment compensation | Aggregate, ports/adapters, Saga, outbox | Guarded transitions, futures/events, command idempotency |
| [Inventory Reservation](../Problems/inventory_reservation_service/) | Multi-SKU all-or-none and no oversell | Strategy, Repository/Unit of Work | Lock/transaction boundary, deterministic races, TTL |
| [Promotion Engine](../Problems/coupon_promotion_engine/) | Deterministic eligibility/benefit/stacking and atomic usage | Specification/Composite, Strategy | Immutable money, atomic redemption, concurrent final use |
| [Notification Framework](../Problems/notification_framework/) | Accepted is not delivered; retry and fallback are policy | Adapter, Strategy, Observer, outbox | Scheduling, callbacks, dedupe, bounded retry |
| [Payment Processing](../Problems/payment_processing_service/) | Unknown outcomes must not duplicate money movement | Aggregate, Adapter, State, idempotent command | `CompletableFuture`, per-payment serialization, callback races |
| [Warehouse Fulfilment](../Problems/warehouse_fulfilment_domain/) | Atomic task claim, scan dedupe, picked/packed quantity | Strategy, State, ports/adapters | Concurrent claim race, immutable audit/outbox |
| [Rate Limiter](../Problems/rate_limiter/) | Refill/check/consume is one atomic per-key operation | Strategy, Factory/resolver | Per-key locks, monotonic clock, concurrent final token |

The runnable [Java concurrency patterns](../Concurrency/interview_patterns/) connect the topics with
non-blocking retry, bounded producer-consumer dispatch, async observers, backpressure, and failure
propagation.

## Principles to demonstrate, not recite

### Encapsulation

The aggregate owns its transition. Use `payment.capture(amount)` or `order.confirm(paymentId)`, not
public setters. Validation and mutation happen in one consistency boundary so callers cannot create
half-valid state.

### Single responsibility

Separate domain decisions from orchestration and infrastructure:

```text
Controller / consumer
    -> application service / command handler
        -> aggregate + policies
        -> repository + outbox + external ports
```

An entity protects business invariants. A service coordinates a use case. An adapter translates a
provider protocol. A repository persists the aggregate. None needs to own all four jobs.

### Open/closed and dependency inversion

Abstract where variation is demonstrated: payment provider, notification channel, allocation rule,
promotion benefit, or rate-limit algorithm. High-level workflow depends on those ports. Do not turn
stable value objects into interfaces or create one implementation interfaces merely to name SOLID.

### Composition over inheritance

A promotion composes `Condition`, `Benefit`, and `UsagePolicy`; it should not create classes such as
`VipSummerBogoPromotion`. A notification composes channel/provider/retry/fallback policies. This
keeps change axes independent and tests small.

### Immutability

Use records or final fields for commands, snapshots, money, IDs, and events. An order stores the
price agreed at purchase. Money uses integral minor units and an explicit currency—never `double`.

## Concurrency talk track

Always answer these four questions:

1. Which invariant can concurrent commands violate?
2. What exact operation is atomic?
3. What is the in-memory mechanism and its production database/distributed equivalent?
4. What test forces the race and asserts the invariant?

Examples:

- Inventory: validate and increment every SKU in one transaction; sorted locks are the local model.
- Payment: reserve capture/refund amount before provider I/O; serialize per payment, not globally.
- Promotion: atomically consume usage and create unique redemption by order/promotion.
- Warehouse: compare-and-set task ownership plus unique scan ID.
- Rate limiter: refill/check/decrement under one per-key lock or Redis Lua script.

`ConcurrentHashMap` alone does not protect invariants spanning several fields or keys. An async API
also does not create thread safety: completion callbacks may run concurrently on arbitrary threads.

## Retry and unknown outcome

The main rule is:

```text
Retry a transient operation only when repeating it is safe under the same idempotency identity.
```

Use capped exponential backoff with jitter and a total deadline. Respect provider `Retry-After`.
Do not retry validation errors, authentication/authorization failures, or business declines.

For a payment timeout, do not infer that no charge occurred. Keep the operation pending/unknown,
query or retry using the same provider key, accept signed callbacks idempotently, and reconcile.
For message delivery, distinguish provider acceptance from eventual delivery.

Stripe documents idempotency for safe request retry:
[Stripe idempotent requests](https://docs.stripe.com/api/idempotent_requests).

## Producer-consumer and Observer

A credible producer-consumer answer specifies:

- bounded queue and full-queue policy;
- number of consumers/in-flight limit;
- message identity and consumer idempotency;
- graceful shutdown and poison/dead-letter behavior;
- retry scheduling without sleeping workers;
- ordering scope, if any;
- durable outbox/inbox substitution for production.

Observer is useful for local reactions and projections. It does not make an event durable. Business
events that must survive a crash are written to a transactional outbox and delivered at least once;
consumers deduplicate by stable event ID and reject aggregate-version regression.

## Testing that sounds senior

Name four groups for every design:

1. Valid and invalid state transitions.
2. Exact boundaries: final stock unit, exact refill instant, full refund, discount equal to total.
3. Concurrency: start contenders with a latch/barrier and assert exactly one winner or a conserved
   total—never thread execution order.
4. Failure/idempotency: duplicate command/callback, timeout then late success, retry exhaustion,
   stale/out-of-order event, and compensation failure.

Inject `Clock` or a monotonic time source; avoid `Thread.sleep()` in deterministic tests.

## Red flags

- Starting with microservices before defining the domain invariant.
- Public status setters or an anemic aggregate controlled by a giant manager class.
- Pattern-name dumping without the change that requires each abstraction.
- One global lock when unrelated customers/SKUs/payments can proceed independently.
- Unbounded thread pools/queues or blocking `CompletableFuture.join()` inside services.
- Retrying every exception or generating a fresh payment key on each attempt.
- Calling queue delivery exactly once rather than making the business effect idempotent.
- Treating in-process Observer publication as a durable integration guarantee.

## Mock-interview order

1. Promotion Engine: clean composition and deterministic pricing.
2. Rate Limiter: focused concurrency and algorithm trade-offs.
3. Inventory Reservation: multi-key atomicity and expiry.
4. Notification Framework: producer-consumer, retry, callbacks, and fallback.
5. Payment Processing: hardest idempotency/unknown-outcome problem.
6. Order Management: integrates inventory, payment, events, and compensation.
7. Warehouse Fulfilment: rich workflow, allocation, scans, and concurrent task ownership.

For each mock, stop at 60 minutes, compile the solution, and record one concrete improvement under
requirements, model, concurrency, failure semantics, Java clarity, and communication.
