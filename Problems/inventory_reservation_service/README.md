# Inventory Reservation Service

## Problem Description

Design a service that places temporary holds on sellable inventory so concurrent
checkout attempts cannot oversell the same stock. A reservation may be confirmed,
released explicitly, or expire after a configurable time-to-live (TTL).

The service owns logical availability and reservations. It does not own physical
pick, pack, or shipment work inside a warehouse.

## 60-Minute Senior/Staff Interview Guide

Use this section to run the interview. The requirements and Java 17 solution below
hold the full problem and implementation detail; the hour should be spent proving
correctness at reservation boundaries, not repeating that material.

### Candidate-facing question

> Design the domain and application layer for an inventory reservation service.
> Stock is identified by SKU and location. A request may allocate across locations,
> but a multi-line reservation is all-or-none. An active hold can be confirmed,
> released, or expire at a precise TTL boundary. Prevent overselling under
> concurrency; define replay and conflict semantics; and explain what happens when
> a caller times out after an unknown database outcome or confirmation races expiry.
> Begin with an in-memory Java design, then evolve its transaction boundary to
> multiple service instances. Physical warehouse work and a generic distributed
> systems tour are out of scope.

### Minute-by-minute plan

| Time | Candidate objective | Interviewer signal |
| --- | --- | --- |
| 0–5 | Define inventory ownership and reserve/confirm/release meanings | Does not confuse a temporary hold with sale or warehouse picking |
| 5–12 | Clarify allocation, TTL boundary, atomicity, and idempotency | Turns ambiguous words into observable rules |
| 12–20 | Model balances, reservation, allocations, status, clock, and versions | Encapsulates counters and terminal transitions |
| 20–29 | Specify commands/results and mutation ownership | Includes expiry/replay/conflict response semantics |
| 29–39 | Implement or pseudocode atomic multi-line reserve | Validates a pure allocation before mutating all affected balances |
| 39–48 | Resolve final-unit, confirm/expiry, retries, and crash races | Identifies a linearization point and one winner |
| 48–54 | Test invariants and concrete scenarios | Uses barriers/fake time rather than probabilistic sleeps |
| 54–58 | Evolve to database transactions, workers, and hot-key handling | Preserves safety across instances without hand-waving a distributed lock |
| 58–60 | Summarize trade-offs and remaining limitation | Knows where strict all-or-none semantics stop scaling cheaply |

### Clarifying questions and strong assumptions

| Ask | Strong default answer for this interview |
| --- | --- |
| What is the stock identity? | `(sku, locationId)`. SKU-only availability is an aggregate query, not the write key. Quantities are non-negative integers. |
| Who changes on-hand stock? | A trusted inventory-adjustment feed owns replenishment/correction. This interview implements reservation mutations and assumes initial balances are supplied. An adjustment may not make `onHand < reserved`. |
| May allocation split a SKU? | Yes, across eligible locations, and the exact immutable allocation is returned. A policy may prohibit splits without changing lifecycle logic. |
| Is partial success allowed? | No. Every requested unit across all lines is held in one commit or none is. Backorders and partial allocation are extensions. |
| What does confirm do? | It consumes the held units exactly once: both `onHand` and `reserved` fall by the allocation. It does not perform pick/pack/shipping. |
| What is the exact expiry rule? | At `now >= expiresAt`, the reservation is expired and cannot confirm. All decisions use one clock/time authority. |
| Can TTL be extended? | Not in the base scope. Renewal would be a versioned command with a maximum hold policy and its own idempotency key. |
| What is the idempotency scope? | Tenant + operation + key. Same canonical input replays the stored result; different input is a conflict. Definitive failures are replayed if that is the published contract. |
| What consistency is promised? | Reservation writes are strongly consistent across every affected balance. Availability reads may be cached only if labeled approximate and never used to authorize a hold. |
| Must cleanup run exactly at expiry? | No. Expiry is a state rule, not a scheduler guarantee. A command/read encountering a due active hold reconciles it; a background sweeper reclaims capacity promptly. |

### Interview scope

**Required behavior:** atomically reserve requested SKU quantities, return immutable
location allocations and expiry, confirm, release, expire, query a reservation, and
report authoritative availability. Allocation policy varies behind a narrow port.
The detailed functional list below remains authoritative.

**Quality requirements:** `available` never negative; one winner for a scarce unit
and for confirm-versus-expiry; multi-line all-or-none mutation; deterministic TTL;
deadlock-safe acquisition; durable operation-scoped idempotency; bounded retry under
contention; auditable terminal reason; crash-safe cleanup; and metrics for active,
expired-but-not-swept, conflicted, and hot-key operations. Reservation safety is
CP: when commit outcome is unknown, do not guess that stock is free.

**Out of scope:** catalog metadata, forecasting, procurement, warehouse bins and
picks, shipping, payment/order orchestration, lot/serial tracking, returns, global
search, partial fulfilment, and a cross-region cache design. Stock adjustments may
be named as an administrative port but need not be fully implemented.

### Core ownership boundaries

| Owner | Owns | Must not decide |
| --- | --- | --- |
| `InventoryBalance` | `onHand` and `reserved` for one `(sku, location)` plus its version | Which order or payment deserves stock |
| `Reservation` aggregate | Request, immutable allocations, expiry, status, version, and terminal reason | Live availability outside its recorded allocations |
| Reservation application service | Command receipt, transaction over all touched rows, lifecycle transition, and result | Allocation ranking rules or warehouse execution |
| `AllocationStrategy` | Pure choice over eligible balance snapshots and request constraints | Counter mutation, locking, TTL, or reservation status |
| Repository/unit of work | Atomic balances + reservation + idempotency receipt + outbox commit | Domain-policy decisions |
| Expiry worker | Finds due active reservations and invokes the same guarded expiry transition | A second “fast path” that bypasses lifecycle rules |
| Order/checkout client | Chooses request/key and reacts to reservation result | Direct balance mutation |
| Warehouse service | Physical receipt, pick, pack, and shipment | Logical checkout hold lifecycle |

### Commands, APIs, and response semantics

| Command | Essential input | Successful result |
| --- | --- | --- |
| `reserve` / `POST /reservations` | Positive SKU quantities, eligible locations/constraints, bounded TTL, `Idempotency-Key` | `201 ACTIVE`; reservation ID, `expiresAt`, immutable allocations, and version. Exact replay may be `200` with `replayed: true`. |
| `confirm` / `POST /reservations/{id}/confirmation` | Key and optional `expectedVersion` | `200 CONFIRMED`; consumed allocations and version |
| `release` / `POST /reservations/{id}/release` | Key, reason, optional version | `200 RELEASED`; restored availability and version |
| `expireDue` (internal) | Time authority, batch cursor | Count/results for guarded `ACTIVE -> EXPIRED` transitions; safe to retry |
| `getReservation` / `GET /reservations/{id}` | Reservation ID | Status, expiry, allocations, version, and terminal reason |
| `getAvailability` / `GET /availability?sku=&location=` | SKU/location filter | Authoritative `onHand`, `reserved`, `available`, and as-of version/time |

Return typed outcomes: `Applied`, `Replayed`, `Insufficient`, `Expired`,
`TerminalConflict`, `VersionConflict`, and `IdempotencyConflict`. Suggested HTTP
mapping is `404` for unknown reservation, `409` for insufficient stock/stale
version/terminal conflict/key conflict, and `422` for invalid quantities or TTL.
Confirming an already confirmed reservation and releasing an already released one
may return the current terminal snapshot as a natural no-op; attempting the other
terminal transition is a conflict. A request timeout after possible commit is
resolved by retrying/querying with the same key, never by creating a fresh hold.

### State machine and invariants

```text
                    confirm
             ┌────────────────> CONFIRMED
             │
ACTIVE ──────┼──── release ───> RELEASED
             │
             └── now >= expiresAt ──────> EXPIRED
```

All terminal states are final. Confirm and the expiry rule use the same serialized
transition; a cleanup job does not own a looser state machine.

For each `(sku, location)` and reservation:

- `onHand >= 0`, `reserved >= 0`, `reserved <= onHand`, and
  `available = onHand - reserved`.
- Each request quantity is positive. Allocations are positive, reference eligible
  locations, and sum exactly to each requested SKU; no extra SKU may appear.
- Reserve increments `reserved` for every allocation in one commit. Confirm
  decrements both `onHand` and `reserved`; therefore confirmation itself does not
  change `available`. Release/expiry decrements only `reserved` and restores
  availability.
- A failed reserve changes no balance and creates no active reservation. A terminal
  action changes each allocated counter exactly once.
- `now >= expiresAt` is expired. Time is read once at the transition's linearization
  point; production instances use one authoritative source, while tests inject a
  clock.
- Reservation request, allocation, creation time, and expiry are immutable in the
  base design. Status, version, and audit advance atomically.
- An on-hand correction that would cross below active reservations is rejected or
  routed to an explicit shortage workflow; it never silently violates the balance
  invariant.

### Expected solution and design-principle reasoning

**Expected shape.** `InventoryBalance` encapsulates counters; `Reservation` is an
immutable/tightly guarded aggregate with `confirm`, `release`, and `expire(now)`.
`InventoryReservationService` is intentionally a transaction script across several
balance aggregates: it obtains consistent snapshots, asks a pure
`AllocationStrategy`, validates the entire allocation, and commits balances,
reservation, and command receipt together. The lower **Architecture and Design
Choices** section shows the in-memory reference implementation of this boundary.

| Principle/pattern | Why it fits here | Boundary that prevents over-design |
| --- | --- | --- |
| SRP | Balance protects counters, reservation protects lifecycle, strategy ranks locations, service owns atomic orchestration | Allocation code cannot mutate stock or schedule expiry |
| OCP + Strategy | First-fit, nearest, cheapest, and safety-stock allocation genuinely vary | Strategy receives immutable snapshots and must return a complete proposal; lifecycle stays fixed |
| ISP | Separate allocation, clock, ID, repository, and event contracts expose only required behavior | Avoid a general inventory god-service or CRUD setters |
| DIP | Domain/application layers depend on those ports, not timers, SQL, or a framework | In-memory and database adapters obey identical transition semantics |
| LSP | Every strategy must be pure, deterministic for equal input/tie-breaks, eligible-only, and never over-allocate | A “strategy” that reserves as it scans is not substitutable |
| Aggregate + Value Object | Reservation ID, inventory key, quantity, allocation, and expiry make invalid data hard to represent | Do not place all SKUs in one giant aggregate; the service transaction coordinates touched balances |
| Repository + unit of work | Multi-balance all-or-none is a storage transaction, not a sequence of saves | Repository methods must not expose a partially committed reservation |

**Alternatives rejected:** `ConcurrentHashMap` alone for a compound check-then-act;
one independent lock/commit per SKU without ordered acquisition and rollback;
decrementing on-hand at reserve time and then inventing ambiguous release logic;
one in-memory timer per reservation as the source of truth; a distributed lock that
is not fenced by the database write; Singleton service state; and a GoF State class
for four small guarded transitions. A full State pattern is justified only if
terminal states gain substantial distinct behavior. Publication to other services
uses an outbox, not an in-process Observer mistaken for durable delivery.

**Testability and extension:** inject `Clock`, ID generator, strategy, and repository;
keep allocation pure. Table-test boundary instants and terminal commands;
property-test counter conservation and allocation sums; use barriers/latches to
force final-unit and confirm/expiry races; fault-inject before and after commit; and
contract-test every strategy. New allocation policies, renewal, safety stock, or
backorders enter through explicit policy/command types without giving callers
counter setters.

### Concurrency, idempotency, and failure reasoning

**Single-process linearization.** One fair transaction lock is a valid exercise
choice: read all balances, compute and validate a complete allocation, mutate every
counter, insert the reservation/receipt, then unlock. Fine-grained locks are valid
only with a canonical `(sku, location)` order and an atomic rollback path.

**Multi-instance linearization.** In a relational implementation, start a short
transaction, insert/claim the unique idempotency receipt, lock affected balance rows
in sorted key order (or use serializable conditional writes), allocate from those
snapshots, validate, update counters, and insert the reservation. Confirm, release,
and expiry lock the reservation plus its allocation rows and conditionally update
`WHERE status = 'ACTIVE'`. Retry deadlock/serialization failures with a bounded,
jittered policy. Never keep those locks across order/payment network calls.

| Race/failure | Required reasoning |
| --- | --- |
| Two callers want the last unit | Both checks occur inside the same write boundary; at most one increments `reserved`, and the loser gets `Insufficient` |
| Client times out after reserve commit | Retry with the same key finds the receipt/reservation and returns it; a new key could create a second valid hold and is unsafe |
| Database commit returns unknown | Query the idempotency key on a new connection before retrying mutation; preserve an `IN_PROGRESS` receipt/recovery path if needed |
| Confirm races expiry | Both use the same lock/conditional transition and authoritative `now`; at `now >= expiresAt`, confirm must expire/reject. Exactly one counter update commits |
| Release races expiry | One `ACTIVE -> terminal` compare-and-set wins; the other returns the winning snapshot and does not decrement `reserved` again |
| Expiry worker dies mid-batch | Reservation status and counter restoration share a transaction. Uncommitted work is retried; committed work fails the next `ACTIVE` predicate |
| Allocation sees stale data | Treat its output as a proposal and revalidate under the transaction; do not trust a cache to authorize stock |
| Outbox/event delivery repeats | Publish after the local commit through an outbox; consumers deduplicate by reservation event ID/version |

A scalable sweeper selects due active reservations in small batches with row locks
or `SKIP LOCKED`, invokes the same transition, and measures expiry lag. Reads and
mutating commands may lazily expire a due reservation so delayed cleanup never lets
it confirm. Persist definitive failures for exact replay; a caller uses a new key to
request a deliberate fresh attempt after restock.

### Concrete walkthrough

1. At 10:00, `(SKU-A, MUM)` has `onHand=3,reserved=0`, `(SKU-A, BLR)` has
   `2,0`, and `(SKU-B, MUM)` has `1,0`. `reserve(K-7, A×4, B×1, TTL=5m)`
   proposes A×3 MUM, A×1 BLR, B×1 MUM.
2. One transaction validates every row, increments the three reserved counters,
   and stores active R-7 with `expiresAt=10:05`. The response is lost. Retrying K-7
   returns R-7; it does not allocate again.
3. A competing A×2 request now sees only one A available and fails without holding
   that one unit—the multi-line/request result is all-or-none.
4. At exactly 10:05, confirmation and the sweeper race. Whichever locks R-7 first
   applies the same boundary rule: it transitions R-7 to `EXPIRED` and decrements
   reserved once. The other observes `EXPIRED`; no stock is consumed.
5. A separate R-8 confirmed at 10:04:59 decrements both on-hand and reserved for
   its allocation. Its available quantity is unchanged by confirmation, which is a
   useful conservation check.

### Senior and Staff expectations

| Concern | Strong Senior answer | Additional Staff-level answer |
| --- | --- | --- |
| Atomicity | One lock/unit of work covers all requested lines | Compares row locks, serializable isolation, and conditional writes using contention evidence |
| Expiry | Injected clock, one boundary rule, guarded sweep | Database time authority, `SKIP LOCKED` batches, expiry-lag SLO, repair/backfill tooling |
| Idempotency | Operation-scoped key/fingerprint and stored result | Retention/archival, tenant scoping, unknown-commit recovery, abuse/cardinality controls |
| Hot inventory | Sorted locks and bounded retries | Admission control, wait/fairness policy, shard ownership, and hot-SKU telemetry |
| Scale boundary | Moves in-memory state into transactional storage | States that strict cross-shard all-or-none needs a coordinator/transaction or a documented semantic relaxation |
| Multi-region | Keeps one authoritative write path | Chooses a home region/partition for scarce stock; remote reads may be approximate, reservations may not |
| Evolution | Strategy port adds location policy | Versioned policy inputs/results, replay-safe migrations, audit/event schema compatibility |

### Interviewer follow-up questions and concise strong answers

| Question | Strong answer |
| --- | --- |
| Why is `ConcurrentHashMap` insufficient? | Availability check plus updates across multiple keys is a compound invariant; thread-safe individual operations are not an atomic transaction. |
| Why keep both `onHand` and `reserved`? | A hold should reduce availability without claiming physical consumption. Confirmation then consumes both counters; release changes only the hold. |
| Does confirmation reduce availability again? | No. Before confirm the units are already unavailable; decreasing on-hand and reserved by the same quantity preserves `onHand - reserved`. |
| What happens exactly at `expiresAt`? | Expiry wins because the rule is `now >= expiresAt`. Confirm first reconciles expiry under the same write boundary. |
| Why not schedule one timer per hold? | Timers are lost on restart, expensive at scale, and can race. Persist expiry; use guarded lazy expiry plus a batched sweeper. |
| How do you prevent deadlock? | Acquire all inventory keys in canonical order, keep the transaction short, and retry deadlock victims with a bound. |
| Can allocation run before locking? | It may make a hint, but the final proposal must be recomputed or fully revalidated against locked/versioned rows. |
| What if the idempotency key is reused with a changed SKU quantity? | Return `IdempotencyConflict`; silently returning the first reservation hides a dangerous caller bug. |
| Can you promise exactly-once expiry? | Delivery may repeat. The guarded terminal transition and same-transaction counter change make its business effect occur once. |
| How do you handle a negative stock correction with active holds? | Reject/quarantine it or run an explicit shortage-resolution workflow; never force `onHand < reserved`. |
| How would you shard multi-SKU reservations? | Preserve strict semantics by co-locating/centrally coordinating touched keys. Otherwise use provisional per-shard holds and admit that atomic availability/latency semantics changed. |
| What should page first in production? | Oversell invariant breach immediately; then expiry lag, hot-key conflict/retry exhaustion, and active holds older than their policy TTL. |

### Red flags

- Decrements stock after an unlocked `if (available >= requested)` check.
- Updates each SKU independently and calls compensating increments “atomic”.
- Uses seat/SKU status alone instead of quantities and immutable allocations.
- Lets allocation policy mutate counters or return an unvalidated partial proposal.
- Uses wall-clock calls in multiple classes or leaves `now == expiresAt` undefined.
- Lets a delayed timer release a reservation that was confirmed concurrently.
- Retries an unknown reserve with a fresh key or treats a timeout as a failed commit.
- Says a distributed lock guarantees correctness without a fenced/conditional
  database write.
- Claims arbitrary cross-shard all-or-none reservation with no coordinator or
  semantic trade-off.
- Adds patterns by name but cannot produce a final-unit or boundary-time test.

### Scoring rubric

| Area | Points | Senior bar | Staff-strength evidence |
| --- | ---: | --- | --- |
| Scope and semantics | 10 | Defines hold, confirm, release, TTL, and exclusions | Names safety/availability and cross-shard semantic limits |
| Model and invariants | 20 | Correct counters, immutable allocation, guarded terminal state | Conservation properties and shortage/adjustment evolution |
| API and ownership | 15 | Precise typed outcomes and mutation boundaries | Versioned contracts, authoritative-vs-approximate reads, repair surface |
| Atomicity and races | 25 | Valid all-or-none transaction and deterministic race winners | Isolation/hot-key/deadlock analysis across instances |
| Idempotency and recovery | 15 | Fingerprint, stored replay, unknown-commit resolution | Retention, sweeper recovery, outbox/inbox, operational SLIs |
| Principles, tests, evolution | 15 | Justified SOLID/Strategy choices and deterministic tests | Migration, sharding, policy versioning, and rejected alternatives |

`70–84` is a solid Senior solution; `85+` with evidence in at least three
Staff-strength columns is Staff-level. Any design that can oversell the final unit
or decrement a hold twice is a correctness failure regardless of total.

### Interview variations and expected solutions

These cuts synthesize the relevant material in all three pinned clones. No source
contains the complete SKU/location/TTL/idempotency service, so unlicensed code is
summarized, language/exact duplicates are collapsed, and incomplete models are
explicitly treated as interview inputs. Each **Python code fit** note identifies the
small live-coding adaptation; database-scale concerns stay in the design discussion.

#### Variation 1 — Generic inventory system becomes a reservation service

- **Candidate prompt/scope delta:** The source says only “Inventory System.” Narrow
  it to the canonical hold lifecycle above and require the candidate to define
  `onHand`, `reserved`, `available`, allocation, TTL, and atomic multi-line behavior.
- **Expected Senior solution:** Encapsulated balances, immutable reservation
  allocation, guarded terminal transitions, injected clock/strategy, one transaction
  boundary, and command receipts with input fingerprints.
- **Staff extension/trade-offs:** Multi-instance row/conditional locking, hot-key
  control, expiry-lag SLO, reconciliation of unknown commits, and an explicit limit
  on strict cross-shard atomicity.
- **Canonical runnable solution / Python code fit:** The Python core applies directly:
  pure allocation, atomic counters, ordered locks, precise expiry, terminal guards,
  and replay. Replace its maps/locks with a repository unit of work in production.

Provenance: `References/low-level-design-primer/questions.md` and
`References/PRIMER_QUESTION_INDEX.md` ([local prompt](../../References/low-level-design-primer/questions.md),
[local index](../../References/PRIMER_QUESTION_INDEX.md)); [pinned prompt line 134
at `49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L134).
This is a **one-line prompt fragment**, not a solution.

#### Variation 2 — Atomic checkout stock deduction

- **Candidate prompt/scope delta:** Place an order only when every cart line is in
  stock, then deduct all quantities immediately. There is no temporary hold, TTL,
  location allocation, or separate confirmation.
- **Expected Senior solution:** Snapshot the requested quantities, validate and
  decrement them inside one serialized transaction, reject all lines on any
  shortage, and make placement idempotent. `ConcurrentHashMap` does not make the
  compound operation atomic.
- **Staff extension/trade-offs:** Decide whether immediate deduction is acceptable
  during payment uncertainty; otherwise introduce the canonical hold. Add durable
  command receipts, conditional updates, hot-SKU metrics, and order compensation.
- **Canonical runnable solution / Python code fit:** Adapt Python by collapsing
  proposal validation and confirmation into one locked command. Keep its all-or-none
  mutation and receipts; omit TTL/release only when no checkout window exists.

Provenance: `References/awesome-low-level-design/problems/online-shopping-service.md`
and `References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/services/InventoryService.java`
([local problem](../../References/awesome-low-level-design/problems/online-shopping-service.md),
[local Java input](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/services/InventoryService.java));
[pinned problem](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/problems/online-shopping-service.md)
and [pinned Java input at `fc26e40`](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/onlineshoppingservice/services/InventoryService.java).
Other language directories are **ports/repetitions** of the broad shopping prompt.

#### Variation 3 — Timed movie-seat hold

- **Candidate prompt/scope delta:** Replace fungible SKU quantities with exclusive
  `(showId, seatId)` units. Lock all chosen seats for one user, expire after a short
  payment window, then book or release them atomically.
- **Expected Senior solution:** Model a reservation ID/owner and immutable seat set,
  make lock acquisition all-or-none per show, return failure explicitly, inject the
  clock, and guard confirm/expiry under the same lock. A scheduled callback is only
  cleanup, not the truth.
- **Staff extension/trade-offs:** Persist holds and expiry, coordinate payment by
  stable key, use a batched sweeper and authoritative time, partition by show, and
  define fairness/admission control for blockbuster contention.
- **Canonical runnable solution / Python code fit:** Map Python's `InventoryKey` to
  show/seat with quantity one; reserve, release, expiry, ordered locks, and tests
  apply directly. Confirmation books the seat; payment stays outside the aggregate.

Provenance: the Java/Python/C# seat-lock files under
`References/awesome-low-level-design/solutions/` and
`References/awesome-low-level-design/problems/movie-ticket-booking-system.md`
([local problem](../../References/awesome-low-level-design/problems/movie-ticket-booking-system.md),
[local solution root](../../References/awesome-low-level-design/solutions/),
[local Java](../../References/awesome-low-level-design/solutions/java/src/movieticketbookingsystem/SeatLockManager.java),
[local Python](../../References/awesome-low-level-design/solutions/python/movieticketbookingsystem/seat_lock_manager.py),
[local C#](../../References/awesome-low-level-design/solutions/csharp/movieticketbookingsystem/SeatLockManager.cs));
[pinned problem](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/problems/movie-ticket-booking-system.md)
and [pinned solution root at `fc26e40`](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions).
The three lock managers are **language ports**, collapsed into one variation.

#### Variation 4 — Immediate movie-seat booking

- **Candidate prompt/scope delta:** Atomically check and mark selected seats booked
  with no temporary hold. Cancellation releases booked seats; payment-window expiry
  is absent.
- **Expected Senior solution:** One per-show/transactional critical section around
  check-and-mark, immutable booking lines, guarded cancel, and idempotent booking ID.
  Return a typed conflict when any seat is unavailable.
- **Staff extension/trade-offs:** Explain the poor payment/user experience without a
  hold, or coordinate payment compensation durably. If the semantics remain direct,
  prefer shorter critical sections and partition by show.
- **Canonical runnable solution / Python code fit:** Adapt Python to perform its
  allocation proposal and consuming counter update inside one lock boundary. Remove
  `ACTIVE`/TTL, but retain all-or-none validation, receipts, and terminal guards.

Provenance: `References/awesome-low-level-design/solutions/cpp/movieticketbookingsystem/`
and `References/awesome-low-level-design/solutions/golang/movieticketbookingsystem/`
([local C++](../../References/awesome-low-level-design/solutions/cpp/movieticketbookingsystem/),
[local Go](../../References/awesome-low-level-design/solutions/golang/movieticketbookingsystem/));
[pinned C++](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/cpp/movieticketbookingsystem)
and [pinned Go](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/golang/movieticketbookingsystem).
These are **direct-booking language variations**, distinct from the timed ports.

#### Variation 5 — Fair concert reservation and waitlist

- **Candidate prompt/scope delta:** Reserve/purchase named concert seats under heavy
  contention, add fair booking opportunity and a waitlist, and notify the next user
  when capacity returns.
- **Expected Senior solution:** Reuse exclusive-seat holds, define a FIFO or scored
  waitlist policy with deterministic tie-breaks, atomically grant at most one offer,
  and give that offer its own expiry/idempotency key.
- **Staff extension/trade-offs:** Admission queue versus database contention,
  anti-bot quotas, fairness versus premium priority, partitioning by event/section,
  and replay-safe notification/offer acceptance.
- **Canonical runnable solution / Python code fit:** Python supplies the exclusive
  hold and final-unit boundary. Add a separate waitlist/offer aggregate and policy;
  do not put queue ordering inside `Reservation` or weaken stock safety.

Provenance: `References/awesome-low-level-design/problems/concert-ticket-booking-system.md`
and its Java/Python/C++/C#/Go solution directories ([local problem](../../References/awesome-low-level-design/problems/concert-ticket-booking-system.md),
[local solution root](../../References/awesome-low-level-design/solutions/),
[local Java representative](../../References/awesome-low-level-design/solutions/java/src/concertticketbookingsystem/));
[pinned problem](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/problems/concert-ticket-booking-system.md)
and [pinned solution root at `fc26e40`](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions).
The language implementations are **ports/repetitions**; the fairness/waitlist prompt
is the useful scope delta, not a complete reservation answer.

#### Variation 6 — Complete an unfinished BookMyShow hold/payment flow

- **Candidate prompt/scope delta:** Starting from seat/show/booking models and a
  `BookingManager` outline, implement “check all, lock all, pay, confirm or release.”
  Treat the supplied source as a skeleton.
- **Expected Senior solution:** Make `lockSeats` return a reservation/result, reject
  partial locks, add owner and expiry, compensate a definitive payment failure, and
  reconcile a timeout rather than blindly release or recharge.
- **Staff extension/trade-offs:** Persist workflow and holds, stable payment keys,
  recovery after every crash point, show-key partitioning, and explicit ownership
  between booking/payment/reservation services.
- **Canonical runnable solution / Python code fit:** Reuse Python's lifecycle, clock,
  proposal validation, receipts, and seat-shaped keys. Explain the external booking/
  payment process manager; payment does not enter the reservation aggregate.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/BookMyShow/`
and `References/VARIATION_INDEX.md` ([local skeleton](../../References/kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/BookMyShow/),
[local index](../../References/VARIATION_INDEX.md#ticket_master));
[pinned tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LLD-Practice/BookMyShow).
`BookingManager` is unfinished, so this is an **input fragment, not a solution**.

#### Variation 7 — Product stock purchase and return

- **Candidate prompt/scope delta:** Purchase decrements a product count immediately;
  return increments it. Add blacklist and bestseller behavior, but no hold/TTL.
- **Expected Senior solution:** Serialize check-and-decrement, make purchase/return
  command IDs replay-safe, validate return ownership/quantity, and keep immutable
  purchase history rather than nulling it on blacklist.
- **Staff extension/trade-offs:** Separate stock authority, fraud eligibility, and
  analytics projections; use events for bestsellers and define correction/replay
  policy without letting analytics mutate availability.
- **Canonical runnable solution / Python code fit:** Collapse Python's reserve and
  confirm for purchase while keeping its counter and replay invariants. Model return
  as a new stock-adjustment workflow, not release of an old confirmed hold.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/`,
its exact duplicate `.../Order-Booking-System.zip`, and
`References/VARIATION_INDEX.md` ([local tree](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/),
[local ZIP](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System.zip),
[local index](../../References/VARIATION_INDEX.md#order_processing_system));
[pinned tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System).
The ZIP is an **exact duplicate archive**, collapsed here.

#### Variation 8 — Layered ticket-booking applications

- **Candidate prompt/scope delta:** Use the fuller booking/service/controller
  examples to expose shows, availability, booking, cancellation, and payment; then
  add the missing atomic hold semantics rather than scoring package count.
- **Expected Senior solution:** Put check-and-hold in one transaction boundary,
  return immutable booking allocations, guard terminal commands, and keep controller,
  provider, and persistence types outside the domain.
- **Staff extension/trade-offs:** Durable booking saga, payment unknown-outcome
  handling, versioned APIs, partitioning by show, and availability projections whose
  staleness never authorizes a booking.
- **Canonical runnable solution / Python code fit:** Use the Python core as the seat
  provider and adapt only key/request types. Search, controllers, persistence, and
  payment stay outside; retain TTL only when the contract has a payment window.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/bookmyshow/` and
`References/kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking2/`
([local Gradle variation](../../References/kumaransg-LLD/Low_level_Design_Problems/bookmyshow/),
[local controller/provider variation](../../References/kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking2/),
[local index](../../References/VARIATION_INDEX.md#ticket_master));
[pinned bookmyshow](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/bookmyshow)
and [pinned movieTicketBooking2](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/movieTicketBooking2).
They are **distinct layered variations**, not reservation-completeness proofs.

#### Variation 9 — Model/DAO fragments as a design review

- **Candidate prompt/scope delta:** Review the supplied seat/show/booking models or
  DAO sketch, identify which correctness concepts are missing, and design the
  command layer that prevents double booking and expires holds.
- **Expected Senior solution:** Add reservation ID/owner/expiry/status, immutable
  allocations, transaction boundaries, version/idempotency records, typed conflicts,
  and deterministic tests; remove public status/counter mutation.
- **Staff extension/trade-offs:** Compare aggregate versus database row boundaries,
  choose isolation/partition keys, add repair and observability, and explain which
  read models may be eventually consistent.
- **Canonical runnable solution / Python code fit:** Python's model/service/strategy
  split is the missing executable core. Map seat identity to `InventoryKey`; keep
  DAO/controller shapes outside, and treat image/single-file material only as input.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow/`,
`.../oops/bookmyshow2/`, `References/kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking/`,
`References/kumaransg-LLD/Low_level_Design_Problems/System-design/BookMyShow/`, and
`References/VARIATION_INDEX.md` ([local first model](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow/),
[local broader model](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow2/),
[local DAO/model](../../References/kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking/),
[local design fragment](../../References/kumaransg-LLD/Low_level_Design_Problems/System-design/BookMyShow/),
[local index](../../References/VARIATION_INDEX.md#ticket_master));
[pinned first model](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow),
[pinned second model](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow2),
[pinned DAO/model](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/movieTicketBooking),
and [pinned design fragment](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/System-design/BookMyShow).
The last path is a **fragment input**; the others are model/DAO variations, not full
TTL/idempotency solutions.

## Functional Requirements

1. Track on-hand, reserved, and available quantities for each SKU and location.
2. Reserve multiple SKU quantities atomically: reserve every line or none of them.
3. Return a reservation ID, expiry time, allocated locations, and line quantities.
4. Confirm a reservation when the order commits its inventory.
5. Release an active reservation explicitly.
6. Expire abandoned reservations after their TTL and restore availability.
7. Make reserve, confirm, and release operations idempotent.
8. Allow allocation policy to vary without changing reservation lifecycle logic.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `InventoryBalance` | On-hand and reserved quantity for one SKU/location |
| `Reservation` | Aggregate containing requested lines, expiry, and status |
| `ReservationLine` | SKU, requested quantity, and location allocation |
| `ReservationStatus` | `ACTIVE`, `CONFIRMED`, `RELEASED`, or `EXPIRED` |
| `AllocationStrategy` | Selects eligible inventory locations |
| `Clock` | Supplies testable expiry time |

## Business Rules and State Transitions

- Available quantity is `onHand - reserved` and must never be negative.
- Only an `ACTIVE` reservation can become `CONFIRMED`, `RELEASED`, or `EXPIRED`.
- Confirming consumes the held quantity exactly once; releasing or expiring removes
  the hold exactly once.
- A failed multi-line reservation must leave every balance unchanged.
- Expiry decisions must use one injected clock and a documented boundary rule.
- Repeating a completed operation must not change stock a second time.

## Concurrency and Failure Handling

- Concurrent callers competing for the final unit must produce at most one
  successful reservation.
- Confirm racing with expiry must have one deterministic winner.
- A reservation retry after a timeout must resolve through its idempotency key.
- Cleanup must not expire a reservation that was confirmed concurrently.
- Lock ordering or another atomicity strategy must avoid deadlock for multi-SKU
  reservations.

## Demonstration Scenarios

1. Reserve and confirm a single SKU.
2. Fail a multi-line reservation without retaining a partial hold.
3. Let a reservation expire and show the quantity becoming available again.
4. Run two concurrent requests for the final unit.
5. Retry reserve and confirm calls with the same idempotency keys.
6. Demonstrate a confirm-versus-expiry race.

## Extensions

- Partial allocation and backorders
- Safety-stock thresholds
- Cross-location allocation costs
- Distributed storage and locking

## Related Problems

- [Order Management System](../order_processing_system/README.md)
- [Shopping Cart with Expiration](../shopping_cart_with_expiration/README.md)
- [Movie Ticket Booking seat holds](../ticket_master/problem-statement.md)
- [Warehouse Fulfilment Domain](../warehouse_fulfilment_domain/README.md)

---

## Python 3 Reference Solution

### One-hour Python coding scope

**Implement live:** `InventoryKey`, immutable balance/allocation/reservation
snapshots, `ACTIVE` terminal guards, a pure first-fit `AllocationStrategy`, injected
`Clock` and ID port, and `reserve`, `confirm`, `release`, and `expire_due`. The
service should validate a complete multi-SKU proposal before mutation, acquire all
inventory locks in sorted order, and replay operation-scoped receipts. Write the
five deterministic tests for rollback, allocation, replay conflict, exactly-once
terminal counters, exact TTL, and a barrier-driven final-unit race.

**Explain rather than type:** HTTP/status mapping, ORM/repository code, durable
receipts, row locking/serializable retries, multi-instance expiry workers, outbox,
unknown-commit repair, cross-shard trade-offs, hot-key controls, metrics, and tracing.

- [One-hour implementation](python/solution.py)
- [Deterministic tests](python/test_solution.py)

Run with Python 3.9+ and only the standard library:

```bash
cd Problems/inventory_reservation_service/python
python3 test_solution.py
```

## Java 17 Reference Solution

### Implementation Status

| Requirement | Status | Implementation |
| --- | --- | --- |
| On-hand, reserved, and available quantities | Complete | `InventoryBalance` snapshots over encapsulated counters |
| Atomic multi-SKU reservation | Complete | Validate the full pure-policy allocation before changing any counter |
| Confirm, release, and expiry lifecycle | Complete | Immutable `Reservation` with guarded terminal transitions |
| Configurable location allocation | Complete | `AllocationStrategy` port plus deterministic first-fit implementation |
| Thread-safe scarce-resource operations | Complete | One fair transaction lock covers checks and all counter mutations |
| Reserve/confirm/release idempotency | Complete | Operation-scoped key, input fingerprint, and original-result record |
| Testable deterministic TTL | Complete | Injected `Clock`; `now >= expiresAt` means expired |
| Runnable scenarios | Complete | Success, rollback, retries, expiry, final-unit race, and confirm/expiry race |

### Architecture and Design Choices

- `InventoryReservationService` alone mutates stock counters. Callers and allocation
  policies receive immutable `InventoryBalance` snapshots, so they cannot bypass
  `0 <= reserved <= onHand`.
- A fair `ReentrantLock` is the transaction boundary. The service asks the pure
  allocation strategy for all lines, validates that every requested unit is covered
  and still available, and only then increments counters. Thus a failed multi-line
  request leaves every balance unchanged and lock ordering cannot deadlock.
- `FirstFitAllocationStrategy` sorts SKU and location keys for reproducible results
  and can split a SKU across locations. A cost-, distance-, or safety-stock-aware
  policy can implement the same port without changing reservation lifecycle code.
- `Reservation` is immutable and permits exactly one transition from `ACTIVE` to
  `CONFIRMED`, `RELEASED`, or `EXPIRED`. Confirmation subtracts held quantities from
  both on-hand and reserved; release and expiry subtract only from reserved.
- The injected `Clock` defines one boundary rule everywhere: an instant equal to
  `expiresAt` is already expired. Because confirmation and cleanup use the same lock
  and rule, expiry wins deterministically at that boundary regardless of thread
  scheduling, and counters are restored once.
- Idempotency records are namespaced by operation. A repeated successful command
  returns its original immutable result, while reusing its key for different input
  throws `IdempotencyConflictException`.
- The implementation is purposefully in memory. A distributed version needs a
  database transaction or conditional writes around balance rows, reservations,
  and idempotency records; the domain states and boundary rule remain the same.

### Source Structure

```text
com/example/lld/inventory_reservation_service/
├── model/
│   ├── InventoryKey.java
│   ├── InventoryBalance.java
│   ├── Reservation.java
│   ├── ReservationLine.java
│   └── ReservationStatus.java
├── service/
│   ├── InventoryReservationService.java
│   └── FirstFitAllocationStrategy.java
├── port/
│   ├── IdGenerator.java
│   └── AllocationStrategy.java
├── exception/
│   ├── InventoryDomainException.java
│   ├── InsufficientInventoryException.java
│   ├── ReservationNotFoundException.java
│   ├── ReservationExpiredException.java
│   ├── InvalidReservationStateException.java
│   └── IdempotencyConflictException.java
└── demo/
    └── Main.java
```

### Compile and Run

Run from the repository root. The output directory is disposable and is created
under `/private/tmp` so no class files are written into the source tree.

```bash
INVENTORY_OUT="$(mktemp -d /private/tmp/inventory-reservation.XXXXXX)"
find Problems/inventory_reservation_service -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -d "$INVENTORY_OUT"
java -cp "$INVENTORY_OUT" com.example.lld.inventory_reservation_service.demo.Main
```

Key output includes:

```text
Inventory Reservation Service demo passed
  final-unit successes: 1/2
  boundary-race result: EXPIRED
```
