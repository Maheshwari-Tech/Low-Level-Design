# Rate Limiter

## Problem Description

Design a reusable, thread-safe rate-limiting library that decides whether a request
may proceed under one or more configurable quotas. The library must return useful
limit metadata and remain deterministic at time-window boundaries.

Implement at least one policy completely. The design should permit other policies,
such as fixed window, sliding window, and token bucket, without changing callers.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design a reusable rate limiter for API requests. Begin with an in-process Java
> library, support different rules and algorithms, return actionable retry metadata,
> and make concurrent decisions correct. Explain how the same contracts evolve to a
> distributed limiter without rewriting callers.

The expected answer is an object design plus one fully explained request path. Do not
start with Redis or an API gateway; first define what one decision means and which
state makes it correct.

### Suggested interview plan

| Time | Expected output |
| ---: | --- |
| 0–5 min | Clarify deployment boundary, keys, weighted cost, rule ownership, and failure policy |
| 5–12 min | State scope and the exact boundary semantics for refill/reset |
| 12–22 min | Model rule, key, request, decision, policy, state cell, clock, and coordinator |
| 22–32 min | Define the API and walk token-bucket plus fixed-window behavior |
| 32–43 min | Prove the final-token race, per-key isolation, rule update, and cleanup safety |
| 43–52 min | Explain distributed state, hot keys, fail-open/closed, and observability |
| 52–58 min | Defend SOLID/pattern choices and reject unnecessary abstractions |
| 58–60 min | Summarize invariants and the tests that prove them |

### Clarifying questions and strong assumptions

| Ask | Strong working assumption when unspecified |
| --- | --- |
| Library or shared distributed service? | Implement an in-process library; preserve a `StateStore` seam for multi-node evolution |
| What identifies a bucket? | A normalized composite of tenant, principal, endpoint, and rule ID |
| Which policies are required? | Token bucket is primary; fixed window demonstrates policy substitution |
| Are requests weighted? | Yes; cost is a positive integer no greater than configured burst capacity |
| What should a decision return? | Allowed, remaining, relative retry/reset durations, rule version, and reason |
| Can rules change live? | Yes; validate first and document reset semantics for existing state |
| What happens if distributed state is unavailable? | Policy is configured per endpoint; sensitive writes fail closed, low-risk reads may fail open |
| Is wall-clock time required? | No for elapsed quotas; use a monotonic clock and expose relative client durations |

**In scope:** rule registration/update, composite keys, token bucket, fixed window,
weighted requests, concurrent correctness, idle eviction, and deterministic tests.

**Out of scope for the first slice:** authentication, billing, cross-region global
fairness, analytics storage, and HTTP middleware. They consume the limiter contract
but do not belong in its policy state.

### Expected solution and design-principle reasoning

#### Ownership model

| Type | Owned decision |
| --- | --- |
| `RateLimiter` | Resolves a rule, locates/version-checks one state cell, and executes one atomic decision |
| `RateLimitRule` | Immutable validated capacity, period, burst, policy type, and version |
| `RateLimitKey` | Canonical equality for the identity being limited |
| `RateLimitPolicy` | Pure policy transition from current state, request, and monotonic time to result/new state |
| `PolicyState` | Algorithm-specific mutable data owned by exactly one state cell |
| `RateLimitDecision` | Immutable result metadata derived inside the same atomic transition |
| `MonotonicClock` | Testable elapsed time; production and manual implementations share one contract |
| `StateStore` | Optional atomic state boundary when the design moves beyond one process |

The coordinator owns state lookup and synchronization; policies own quota arithmetic.
This prevents a new algorithm from changing key management, configuration, cleanup,
or caller code.

#### SOLID and justified patterns

| Choice | Reasoning |
| --- | --- |
| **SRP** | Rules validate configuration, policies calculate transitions, the coordinator manages lifecycle/locking, and time comes from a clock |
| **OCP + Strategy** | New algorithms implement `RateLimitPolicy`; the stable `allow` API and state-cell protocol remain unchanged |
| **LSP** | Every policy must reject invalid cost consistently, never return negative remaining capacity, and define retry/reset metadata in the same units |
| **ISP** | `MonotonicClock`, policy, and optional state-store ports expose only capabilities needed during a decision |
| **DIP** | Quota logic depends on clock/store abstractions, so boundary and race tests do not sleep or require Redis |
| **Factory/registry** | A small registry maps configured policy type to a tested strategy; callers never instantiate algorithm internals |

Chain of Responsibility is useful when rate limiting is one request-processing stage,
but it is not the limiter algorithm. State objects are useful only when policy state
has real behavior; an enum plus compact state record is otherwise clearer.

#### Interview API contract

```java
RateLimitDecision allow(RateLimitRequest request);
void registerRule(RateLimitRule rule);
void replaceRule(String ruleId, long expectedVersion, RateLimitRule replacement);
int evictIdle(Duration idleFor);
```

`allow` is one atomic read-modify-write for `(ruleId, key)`. An unknown rule and an
invalid request are explicit errors; exhaustion is a normal `allowed=false` result.
The decision includes the applied rule version so a caller can explain behavior
during configuration rollout.

#### Invariants and concurrency proof

1. Remaining capacity is never negative and never exceeds capacity plus burst.
2. A rejected request consumes nothing.
3. Refill/reset is computed before spending; equality at the boundary belongs to the
   new capacity.
4. The state mutation and returned metadata are calculated under the same per-key
   lock or atomic store operation.
5. Unrelated keys use different state cells and do not serialize.
6. Eviction identity-checks the same cell while holding its lock; a concurrent caller
   that observed a removed cell retries against the current map entry.
7. A rule version change resets state exactly once under the key lock in this
   reference design; migration semantics would require a policy-specific converter.

### Concrete walkthrough: sixteen callers, one token

Seed a token-bucket cell with one token, stop refill with a manual clock, then release
sixteen threads through a barrier. All resolve the same cell; one lock holder spends
the token and returns allowed with zero remaining. Every later holder observes zero
and returns the same non-zero retry duration without mutation. The proof is the
invariant and atomic boundary, not a lucky thread schedule.

### Questions an interviewer will ask

| Follow-up question | Expected strong answer |
| --- | --- |
| Why token bucket instead of sliding log? | Token bucket gives bounded state and controlled bursts; a sliding log is more exact but stores timestamps proportional to traffic |
| Why not one `synchronized` method? | It is correct but serializes every key; per-key cells retain safety while allowing unrelated traffic to proceed |
| How does distributed limiting work? | Use one atomic store operation/script keyed by rule and identity; pass server/monotonic-equivalent time consistently and keep the same decision contract |
| How do you handle a hot key? | Sharding cannot split one exact counter safely; use local leases or hierarchical quotas only if bounded overshoot is acceptable |
| What if the clock moves backward? | Elapsed-time math uses a monotonic clock and clamps invalid elapsed values so no capacity is created |
| Can state be evicted at reset time? | Only with a safe identity/locking protocol; TTL alone must not delete a concurrently updated key |
| How do hierarchical limits compose? | Evaluate tenant/user/endpoint quotas in a defined order and commit atomically, or reserve/refund with explicit bounded inconsistency |
| Which metrics matter? | Allowed/denied counts, retry duration, active keys, eviction, rule version, state-store latency/errors, and fail-open decisions |

### Senior and Staff expectations

- A **Senior** answer implements one correct algorithm, a clean Strategy boundary,
  deterministic time, per-key concurrency, client metadata, and boundary/race tests.
- A **Staff** answer additionally defines rule ownership/version rollout, hot-key and
  state-store failure behavior, hierarchical quotas, acceptable accuracy, metrics,
  and a safe local-to-distributed migration without changing consumers.

### Red flags and scoring focus

- Red flags: `double` refill without error bounds, wall-clock elapsed time, global
  synchronization, negative remaining capacity, rejected requests consuming quota,
  cleanup racing active decisions, and saying “use Redis” without an atomic command.
- Score highly for: explicit boundary semantics, a provable final-unit race, SOLID
  tied to policy variation, meaningful response metadata, deterministic tests, and a
  bounded distributed-evolution argument.

### Interview variations and expected solutions

The following are separate ways this problem is asked. They share the stable
`RateLimitRequest -> RateLimitDecision` contract, but each changes the algorithmic or
integration pressure. Language ports and byte-identical copies are collapsed rather
than presented as new solutions.

#### Variation 1 — Reusable API rate-limiting library

**Question.** Design a library that limits by user, key, IP, endpoint, or composite
identity; returns remaining/retry metadata; and permits different algorithms.

**Expected Senior solution.** Use the ownership model above, inject monotonic time,
make one decision an atomic transition of one `(rule, key)` cell, and implement token
bucket completely behind `RateLimitPolicy`. Add fixed window to prove Strategy/OCP,
not because every interview needs two algorithms.

**Staff extension.** Define rule ownership/version rollout, a `StateStore` atomic
operation for multiple nodes, endpoint-specific fail-open/closed behavior, hot-key
limits, and observability without changing callers.

**Canonical code fit.** The Java and Python solutions implement this variation end
to end, including both policies, weighted cost, version reset, eviction, and the
final-token race.

This variation merges the primer's requirements-only [API rate-limiter prompt](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L324) into the canonical solution; the prompt contains no source implementation.

#### Variation 2 — Exact sliding-window limiter

**Question.** For each user, allow at most `N` requests in the preceding duration and
make the decision correct under concurrent calls.

**Expected Senior solution.** Keep an ordered deque of accepted timestamps per key.
Under that key's lock, remove timestamps at or before the declared half-open boundary,
reject when the deque still has `N` entries, otherwise append `now`. Return retry-after
from the oldest retained timestamp. State is `O(N)` per active key; do not use one
global synchronized method or mutate a list while iterating unsafely.

**Staff extension.** Bound memory, define idle cleanup, choose an atomic sorted-set or
script for distributed state, and compare exactness/cost with a sliding-window counter.

**Canonical code fit.** Reuse the coordinator, key, clock, decision, versioning, and
eviction contracts unchanged; add `SlidingWindowPolicy` plus its deque state in both
languages.

This solution is synthesized from Kumar's unlicensed [per-user sliding-window fragment](../../References/kumaransg-LLD/Low_level_Design_Problems/RateLimiter/) ([pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/RateLimiter)); the fragment is retained as a code-review input rather than copied as the answer.

#### Variation 3 — Minimal token bucket

**Question.** Implement a compact token bucket supporting bursts and refill over
elapsed time; explain exact boundary and concurrency behavior.

**Expected Senior solution.** Store capacity, current credits, and last-refill time;
refill before spending and cap with `min(capacity, current + earned)`. Use integer or
fixed-point credits, monotonic time, and a lock/CAS around refill plus spend. A rejected
request does not consume credit. The canonical `TokenBucketPolicy` uses integer
token-nanosecond credits and is the complete solution for this variation.

**Staff extension.** Specify precision/overflow bounds, weighted requests, persisted
rule versions, and the exact atomic script/store representation. Discuss local token
leases only when bounded overshoot is acceptable.

**Canonical code fit.** `TokenBucketPolicy` is complete in both languages and corrects
the fragment's refill precision, cap, time, and synchronization problems.

Kumar's separate unlicensed [`TokenBucket.java` fragment](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/ratelimiter/TokenBucket.java) ([pinned source](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/ratelimiter/TokenBucket.java)) motivates review questions about floating-point drift, incorrect cap direction, wall time, and missing synchronization; those defects are corrected in the canonical implementation.

#### Variation 4 — Rate limiting inside a request-handler chain

**Question.** Place authentication, rate limiting, validation, and business handling
in an extensible request pipeline. Where does the rate limiter belong and what does it
return to the chain?

**Expected Senior solution.** Chain of Responsibility owns request-stage ordering and
short-circuiting; it does not own quota arithmetic. A `RateLimitHandler` derives the
key/rule, calls the stable limiter API, continues on allow, and maps denial metadata to
the transport response. The limiter remains an injected collaborator with independent
state and tests.

**Staff extension.** Define policy lookup, trusted identity ordering, shadow mode,
metrics, and failure behavior. Ensure retries and internal fan-out do not accidentally
consume the wrong external quota.

**Canonical code fit.** Inject the existing `RateLimiter` into a small handler; no
policy or state code moves into the request chain.

The GPL [Java handler fragment](../../References/awesome-low-level-design/design-patterns/java/chainofresponsibility/RateLimitHandler.java) ([pinned source](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/java/chainofresponsibility/RateLimitHandler.java)) supplies this placement variation. Its C++ and C# counterparts are language ports of the same idea, not additional algorithm solutions.

## Functional Requirements

1. Derive a limit key from user, API key, IP address, tenant, endpoint, or a composite.
2. Configure capacity, period, burst allowance, and policy per rule.
3. Return whether the request is allowed, remaining capacity, reset time, and
   retry-after duration.
4. Keep decisions for one key atomic under concurrent calls.
5. Support independent state for unrelated keys.
6. Use an injected monotonic clock for deterministic tests.
7. Apply validated runtime configuration updates with documented semantics.
8. Evict idle key state so memory does not grow without bound.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `RateLimitRule` | Limit, period, scope, and policy configuration |
| `RateLimitKey` | Normalised identity being limited |
| `RateLimitRequest` | Key, rule context, and optional request cost |
| `RateLimitDecision` | Allowed flag and client-facing metadata |
| `RateLimitPolicy` | Algorithm contract |
| `RateLimitState` | Mutable per-key algorithm state |
| `StateStore` | Lookup and atomic update of key state |
| `Clock` | Monotonic, testable time source |

## Business Rules

- Define whether the request at the exact reset/refill boundary belongs to the old
  or new capacity.
- Remaining capacity must never be negative.
- Rejected requests must not consume capacity unless a selected policy explicitly
  documents that behaviour.
- Reject invalid configurations such as negative capacity or non-positive period.
- Rule updates must state whether existing key state is reset, migrated, or retained.
- Decision metadata must be derived from the same atomic state change as the result.

## Concurrency and Failure Handling

- Simultaneous requests for the final unit must allow no more than one request.
- Per-key synchronisation should not serialize unrelated keys.
- Cleanup cannot remove state while a decision for that state is being committed.
- Clock movement or overflow must not create extra capacity.
- If storage is abstracted, define fail-open or fail-closed behaviour explicitly.

## Demonstration Scenarios

1. Allow requests until capacity is exhausted, then reject with retry metadata.
2. Advance the fake clock to the exact boundary and show the documented result.
3. Race concurrent callers for the final token or slot.
4. Show that two different keys do not share capacity.
5. Apply a runtime rule change.
6. Evict idle state without changing active-key behaviour.

## Extensions

- Hierarchical tenant, user, and endpoint quotas
- Weighted requests
- Allowlist and denylist rules
- Distributed state storage

## Related Problems

- [Cache](../cache/README.md)
- [Notification Framework](../notification_framework/README.md)

## Java 17 Reference Solution

**Status:** Complete, runnable Java 17 reference implementation. Token bucket and
fixed window are both implemented, with weighted costs, per-key atomic decisions,
deterministic monotonic time, exact boundary metadata, live rule replacement, idle
eviction, configuration validation, and a real concurrent last-token test.

### Architecture

| Layer | Important types | Design role |
| --- | --- | --- |
| API/domain | `RateLimitRule`, `RateLimitKey`, `RateLimitRequest`, `RateLimitDecision` | Immutable caller-facing configuration and result types |
| Coordinator | `RateLimiter` | Rule registry, per-key state cells, locking, versioned updates, and eviction |
| Policy SPI | `RateLimitPolicy`, `PolicyState`, `PolicyResult` | Pluggable algorithm contract independent of callers and storage |
| Algorithms | `TokenBucketPolicy`, `FixedWindowPolicy` | Complete token bucket and second policy implementation |
| Time | `MonotonicClock`, `ManualMonotonicClock`, `SystemMonotonicClock` | Wall-clock-independent production timing and deterministic examples |

The state key is `(ruleId, RateLimitKey)`. Every cell owns a separate
`ReentrantLock`; the policy mutation and all returned metadata are calculated while
holding that lock. Requests for unrelated keys therefore proceed independently,
while simultaneous callers competing for the final token cannot both succeed.

The token bucket uses integer **token-nanosecond credits** backed by `BigInteger`.
This avoids floating-point refill drift and intermediate overflow. It begins with
`capacity + burstAllowance` tokens, replenishes `capacity` tokens per `period`, and
refills before evaluating a request. Therefore a request exactly on a refill
boundary sees the newly available capacity. Rejected requests consume nothing.

The fixed window is anchored when a key is first seen. `now == windowEnd` belongs
to the new window. Its limit is also `capacity + burstAllowance`.

`resetAtNanos` is expressed in the injected monotonic clock's coordinate system;
`resetAfter` is the portable relative value for clients. `retryAfter` is zero for
allowed requests and exact for denied requests. A monotonic reading is deliberately
not converted to a wall-clock timestamp.

### Runtime Updates and Cleanup

- `updateRule` atomically publishes a new version and uses **reset semantics**.
  Existing key cells reset lazily, under their own lock, on their next decision.
- `evictIdle` identity-checks a cell while holding its lock. A decision that looked
  up a just-removed cell detects removal and retries against a current cell.
- Backward/overflowing elapsed-time arithmetic never grants extra capacity.
- In this in-process implementation no remote store can fail. A distributed
  `StateStore` extension must explicitly select fail-open or fail-closed behaviour.

### Source Tree

```text
src/main/java/com/example/lld/rate_limiter/
├── Demo.java
├── domain/
│   ├── PolicyType.java
│   ├── RateLimitDecision.java
│   ├── RateLimitKey.java
│   ├── RateLimitRequest.java
│   └── RateLimitRule.java
├── exception/
│   ├── DuplicateRateLimitRuleException.java
│   ├── InvalidRateLimitRequestException.java
│   ├── InvalidRateLimitRuleException.java
│   ├── RateLimitException.java
│   └── UnknownRateLimitRuleException.java
├── policy/
│   ├── FixedWindowPolicy.java
│   ├── PolicyResult.java
│   ├── PolicyState.java
│   ├── RateLimitPolicy.java
│   └── TokenBucketPolicy.java
├── service/
│   └── RateLimiter.java
└── time/
    ├── ManualMonotonicClock.java
    ├── MonotonicClock.java
    ├── SystemMonotonicClock.java
    └── TimeMath.java
```

### Compile and Run

From this problem directory:

```bash
mkdir -p out
javac --release 17 -Xlint:all -d out $(find src/main/java -name '*.java' | sort)
java -cp out com.example.lld.rate_limiter.Demo
```

The demo fails fast with `AssertionError` and covers exhaustion plus retry metadata,
the exact token-refill boundary, independent keys, the exact fixed-window boundary,
16 concurrent callers racing for one final token, a live rule update, idle cleanup,
and rejection of invalid configuration.

## Python 3 Reference Solution

The Python 3.9+ answer preserves the same public request/decision model and policy
boundary in an interview-sized module. Frozen dataclasses model immutable
contracts; `TokenBucketPolicy` and `FixedWindowPolicy` implement Strategy;
`RateLimiter` owns versioned rules and per-key `RLock` cells; and `ManualClock` makes
time-boundary tests deterministic. Integer token-nanosecond credits avoid
floating-point drift exactly as in the Java design.

### One-hour Python coding scope

- **Implement live:** immutable rule/key/request/decision dataclasses, injected
  monotonic clock, `RateLimitPolicy`, one complete token-bucket transition, per-key
  locked cells, and the final-token plus exact-boundary tests.
- **Add if time remains:** fixed-window policy, versioned reset, and safe idle
  eviction. These prove the extension seam but are secondary to one correct policy.
- **Explain, do not type:** HTTP middleware, Redis/Lua adapter, configuration plane,
  distributed leases, metrics, and fail-open/closed deployment policy.

```text
python/
├── solution.py       # domain contracts, policies, coordinator, clock, eviction
└── test_solution.py  # boundary, weighted-cost, race, update, and cleanup tests
```

Run from this problem directory:

```bash
cd python
python3 test_solution.py
```

The standard-library test suite releases 16 threads against one final token and
asserts exactly one winner; it also proves the fixed-window boundary, weighted token
refill, backward-clock safety, rule-version reset, and the orphan-cell retry required
for safe idle eviction.
