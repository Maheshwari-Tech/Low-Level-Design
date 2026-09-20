# Engineering Review — Staff Machine Coding Portfolio

## Executive assessment

The three projects are strong reference solutions for a 90-minute machine-coding round. They share a clear dependency direction, keep framework code outside the domain, include runnable APIs and UIs, and test the central behavior. The code now protects domain invariants even when called without HTTP validation and makes process-local race boundaries explicit.

They are not deployable production systems as-is. That distinction is intentional and documented. Storage is volatile, locks do not coordinate across processes, APIs are unauthenticated, and operational controls are minimal.

## Portfolio scorecard

| Area | Assessment | Evidence |
|---|---|---|
| Functional completeness | Strong | Required use cases exist end to end in all three projects |
| Domain modeling | Strong | Aggregates/entities/value objects own invalid-state checks |
| SOLID boundaries | Strong | HTTP → application → domain/port; adapters are composed at startup |
| Pattern judgment | Strong | Repository, policy, factory, snapshot, and DI appear at real variation points |
| Testability | Strong | Repositories are replaceable; booking clock and IDs are injectable |
| Concurrency reasoning | Strong for in-memory scope | Atomic conflict, hold, confirm, and aggregate transitions |
| API hardening | Moderate | Typed validation and domain error mapping; auth/rate limits absent by scope |
| Observability | Gap for production | No structured logs, metrics, traces, or audit sink |
| Durability | Gap for production | In-memory adapters only |
| Deployment readiness | Gap for production | No container, configuration model, secrets, migrations, or SLOs |

## Material findings addressed

### Calendar

- Synchronized list reads with conflict-check writes so callers get a coherent snapshot.
- Extracted working-hours and slot rules into `SchedulingPolicy` instead of hard-coding them in the use case.
- Canonicalized participant identities at the domain boundary.
- Enforced title, participant, local-time, same-day, and working-hours invariants.
- Kept check-and-insert atomic in the repository adapter.

### Movie ticket booking

- Replaced separate held-seat and booked-seat reads with one immutable `SeatInventory` snapshot.
- Revalidated confirmation under the repository write lock.
- Added lazy cleanup of expired holds so inventory does not grow indefinitely during normal traffic.
- Canonicalized seat identifiers and validated show, hold, and booking construction.
- Injected clock and ID generation seams for deterministic behavior and tests.

### Battleship

- Retained the existing aggregate boundary because state transitions and locking are already cohesive.
- Kept ship creation in a factory because fleet configuration is a real construction variation point.
- Documented complexity and the optimistic-versioning path for multi-game production storage.

## Cross-project design rules

1. **Invalid state is rejected close to the model.** HTTP validation improves client errors but is not the business-rule boundary.
2. **Atomicity sits with the adapter that owns the write.** Application-level check-then-save is insufficient.
3. **Read models are snapshots.** Callers should not assemble a logical view from independently changing collections.
4. **Time and identity are injected or normalized.** This keeps behavior deterministic and avoids representation bugs.
5. **Patterns require a reason.** A factory builds configurable ships; a policy varies scheduling rules; repositories isolate persistence and consistency.
6. **Production claims are precise.** A Python lock is thread-safe inside one process, not safe across workers or hosts.

## Production backlog, ordered by risk

| Priority | Capability | Why it comes next |
|---:|---|---|
| P0 | Durable database constraints and transactions | Prevents lost state and cross-instance races |
| P0 | Authentication, authorization, and tenant scoping | Prevents unauthorized reads and writes |
| P0 | Idempotency for mutating APIs | Makes retries safe under network failure |
| P1 | Structured logs, metrics, traces, request IDs | Makes correctness and latency diagnosable |
| P1 | Configuration, secrets, migrations, health/readiness | Enables controlled deployment and rollback |
| P1 | Rate limiting and abuse controls | Protects inventory and scheduling endpoints |
| P2 | Outbox/event delivery | Makes downstream notifications reliable |
| P2 | Load, fault, and concurrency tests | Validates assumptions under contention |

## Review questions for an interview

- Which object owns the invariant, and can another entry point bypass it?
- What exact operation must be atomic?
- What breaks when there are two application processes?
- Which request is safe to retry, and how would the others become safe?
- What is the current big-O cost and expected data size?
- Where would a new storage adapter plug in?
- Which failure should be `400`, `404`, `409`, or `410`, and why?
- What would you instrument before launch?

## Definition of done for this portfolio

- all three applications compile and their automated tests pass;
- required behavior is reachable through the HTTP adapter;
- negative and boundary behavior is tested;
- mutable state cannot leak through public collections;
- concurrency assumptions are documented and protected in the in-memory adapter;
- each design describes invariants, patterns, complexity, trade-offs, and production evolution;
- remaining production gaps are explicit rather than hidden behind “production-ready” language.
