# Warehouse Fulfilment Domain

## Problem Description

Design the warehouse domain that converts confirmed order lines into traceable pick,
pack, and ship work. The design must support multiple warehouses, split shipments,
concurrent workers, scanning validation, and recoverable fulfilment exceptions.

Inventory Reservation owns logical sellable availability. This domain consumes an
approved allocation and owns the physical execution of warehouse work.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design the low-level warehouse fulfilment domain that turns a confirmed order and
> approved inventory allocation into traceable pick, pack, and ship work. An order
> may split across warehouses and packages. Multiple workers scan and claim tasks
> concurrently; stock can be short or damaged; carrier requests can time out and
> callbacks can repeat. Define ownership, APIs, object/aggregate lifecycles,
> invariants, and recovery behaviour. Focus on physical execution, not storefront,
> route optimisation, or inventory-reservation internals.

### Minute-by-minute plan

| Time | Candidate should drive | Interviewer signal |
| --- | --- | --- |
| 0-5 | Restate the flow and distinguish logical reservation from physical handling. | Finds the key domain boundary before classes. |
| 5-12 | Clarify splits, units, scans, claims, cancellation, carrier behaviour, and scope. | Makes explicit assumptions without stalling. |
| 12-21 | Model order lines, allocations, tasks, packages, shipments, and their lifecycles. | Uses quantities and child lifecycles, not one giant status enum. |
| 21-30 | Define commands, queries, ports, identities, and emitted facts. | Contracts carry enough correlation for retries and audit. |
| 30-41 | Solve task/scan/package races, idempotency, short picks, unknown hand-offs, and outbox delivery. | Protects physical truth atomically. |
| 41-49 | Walk a split order through claim contention, duplicate scan, reallocation, and carrier timeout. | Keeps every counter and state consistent. |
| 49-55 | Explain principles, patterns, rejected alternatives, and verification. | Applies patterns only to real variation. |
| 55-60 | Evolve for scale, lots/serials, waves, topology, and operations; summarize guarantees. | Shows Staff-level boundary and migration reasoning. |

### Clarifying questions and strong assumptions

| Ask early | Strong working assumption if unanswered |
| --- | --- |
| What exactly arrives from Inventory Reservation? | A reservation/allocation token approves line quantities and eligible source nodes. Fulfilment materializes warehouse/bin work but never mutates sellable availability directly. |
| May an order split by warehouse, bin, and package? | Yes. A line can split across bins/warehouses and an order across packages/shipments; each physical unit is accounted for once. |
| What quantity types exist? | Integer units with an explicit unit of measure. Catch-weight, lots, serials, and expiry dates are extensions, not silently represented as decimals. |
| Are tasks assigned or self-claimed? | Workers atomically claim available tasks using a versioned lease/claim token; policy may also pre-assign them. |
| What does a scan identify? | A client-generated stable `scanId`, task, claim token, worker, warehouse/bin, SKU/barcode, quantity, and optional package. |
| What follows a short pick? | Persist actual picked quantity and reason, close the original task, then request reallocation of only the remainder; otherwise raise an exception. |
| When is cancellation allowed? | Unpicked work can cancel. Picked work needs an explicit unpack/return-to-bin workflow; sealed or handed-off quantities cannot be erased. |
| How does carrier integration behave? | Label/handoff is idempotent when possible, but may time out or callback later. An ambiguous attempt blocks a second logical shipment until reconciled. |
| What delivery guarantee exists upstream? | Fulfilment facts are published at least once from an outbox; consumers deduplicate by event ID and aggregate version. |

### Scoped requirements and exclusions

In scope:

- idempotent fulfilment creation, allocation materialization, claimable pick work,
  validated/deduplicated scans, short-pick recovery, packing, sealing, carrier
  hand-off, eligible cancellation, and immutable audit;
- multiple warehouses, bins, packages, and shipments per source order;
- concurrency control for task ownership and scarce quantities, plus reliable status
  facts for upstream systems.

Explicitly out of scope:

- purchasing/replenishment, sellable-to-promise math, warehouse layout, robot/route
  optimization, labour scheduling, shipping-rate shopping, carrier tracking after
  hand-off, returns, customs, billing, and customer notifications;
- cross-service two-phase commit and a promise of end-to-end exactly-once messaging.

### Ownership boundaries

| Boundary | Owns | Does not own |
| --- | --- | --- |
| Order management | Commercial order, destination, customer-visible cancellation intent | Bin tasks, scans, package contents, or carrier attempt truth |
| Inventory Reservation | Sellable quantity, reservation token, release/re-reservation decisions | Worker execution or package lifecycle |
| Warehouse fulfilment | Physical allocations, tasks/claims, scans, picked/packed/shipped counters, packages, hand-off attempts, audit | Commercial order state or global availability |
| Warehouse master data | Warehouse/bin/SKU/barcode/lot metadata and eligibility | Consumption of a fulfilment line |
| Carrier adapter | Protocol translation, labels, carrier authentication, normalized hand-off result | Package-content invariants or upstream order status |
| Order/status consumers | Customer-facing projection from fulfilment facts | Rewriting warehouse history |

The approved reservation is an input contract, not shared mutable state. Shortage,
release, and reallocation cross the Inventory boundary as explicit commands/events;
the physical facts already scanned remain owned by Fulfilment.

### APIs and contracts

| Contract | Essential request fields | Result semantics |
| --- | --- | --- |
| `POST /fulfilment-orders` | `sourceOrderId`, reservation token, destination, line IDs/SKUs/quantities; key | Stable `fulfilmentOrderId`; duplicate creation replays, changed fingerprint conflicts |
| `POST /fulfilment-orders/{id}/plans` | approved sources/capacity snapshot or token; key | Deterministic allocation IDs and pick tasks, or explicit shortage |
| `POST /pick-tasks/{id}/claims` | `workerId`, expected task version; key | Claim ID/token, lease expiry, claimed quantity; one winner |
| `POST /pick-tasks/{id}/scans` | `scanId`, claim token, worker, bin, barcode/SKU, quantity | Frozen accepted result; same ID with changed fields conflicts |
| `POST /pick-tasks/{id}/short-picks` | claim token, actual/remainder, reason; key | Closes task and returns reallocation/exception status |
| `POST /packages` and `POST /packages/{id}/contents` | order/warehouse; picked allocation, quantity, package scan ID | Atomically consumes picked-but-unpacked balance |
| `POST /packages/{id}/seal` | weight, dimensions, expected version; key | Immutable manifest suitable for carrier request |
| `POST /packages/{id}/shipments` | carrier/service level; key | Durable shipment attempt: `CONFIRMED`, `REJECTED`, or `UNKNOWN` |
| `POST /fulfilment-orders/{id}/cancellations` | requested line quantities/reason; key | Cancels only quantities eligible at commit time |
| `GET /fulfilment-orders/{id}` | identity | Line counters, task/package/shipment projections, exceptions, versioned history |

Useful domain-owned ports are deliberately narrow:

```text
AllocationPolicy.plan(demand, approvedSources) -> AllocationPlan
InventoryReservationPort.reallocate(reservationId, remainder, excludedSources)
CarrierPort.handoff(ShipmentRequest{stableRequestId, sealedManifest})
    -> CONFIRMED | REJECTED | UNKNOWN
StatusPublisher.publish(FulfilmentEvent)  // driven from a durable outbox
```

An allocation policy is pure and returns a proposal; the service independently
validates totals and source eligibility before committing it. All quantities carry
line ID, SKU, unit of measure, and warehouse/bin provenance. Mutating responses carry
a version so conflicts are visible rather than last-write-wins.

### Invariants and lifecycle

- Per line, `0 <= shipped <= packed <= picked <= allocated` and
  `allocated + cancelled <= requested`. A short pick atomically reduces only the
  unpicked allocation before any replacement allocation is added.
- A task consumes one allocation and satisfies
  `scanned + short + cancelled = task quantity` when terminal. At most one live
  claim epoch owns it; an old lease token cannot scan after reassignment.
- A scan ID is append-only and bound to one fingerprint. Accepted scans adjust the
  task and line exactly once; rejected scans adjust neither.
- A package belongs to one fulfilment order and warehouse. Its contents can use only
  that warehouse's picked-but-unpacked balance; the same quantity cannot be in two
  packages.
- A sealed package has a non-empty immutable manifest and validated weight. It has at
  most one confirmed logical shipment, even if several transport attempts/responses
  are observed.
- Shipped physical quantity is terminal. Order status is a projection across line,
  task, and package states, so one split may be shipped while another is picking or
  in exception.

```text
Task:     AVAILABLE -> CLAIMED -> COMPLETED | SHORT_PICKED | CANCELLED
Package:  OPEN -> SEALED -> SHIPPED
Shipment attempt: PENDING -> CONFIRMED | REJECTED | UNKNOWN
                                                   |-> reconciled to CONFIRMED/REJECTED
Order projection: RECEIVED -> ALLOCATED -> PICKING -> PARTIALLY_SHIPPED -> SHIPPED
                                      \-> EXCEPTION / eligible CANCELLED
```

The diagram is not one monolithic FSM: task and package transitions enforce local
rules, while order status is derived from their quantities and terminal facts.

### Concurrency, idempotency, unknown outcomes, and compensation

| Concern | Strong design |
| --- | --- |
| Two workers claim one task | Conditional update on task state/version (and expired lease) creates a new claim epoch. Exactly one commit wins; the loser receives the current task state. |
| Worker crashes | The lease may expire, but recorded scans remain. Requeue only unfinished quantity under a new claim epoch; stale tokens cannot mutate it. |
| Duplicate or conflicting scan | Insert `(warehouseId, scanId, fingerprint)` and update task/line counters in one transaction. Same fingerprint replays; changed content conflicts and is audited. |
| Concurrent packing | Atomically consume picked-but-unpacked balance using a line/allocation version or unique consumption records. Never rely on a read-then-write count. |
| Short pick races cancellation | Serialize on the affected allocation/line version. The winner records physical truth; the loser reloads and applies only the still-eligible remainder. |
| Carrier timeout | Persist shipment attempt and stable carrier request ID before I/O, mark it `UNKNOWN`, and query/retry with that identity. Do not create another logical shipment or release the package. |
| Duplicate/out-of-order carrier callback | Authenticate, deduplicate event ID, correlate request/package, and make the terminal transition conditional. Contradictions go to reconciliation. |
| Upstream publication | Commit state plus outbox event together. Publish at least once; consumers deduplicate by event ID and use aggregate version to reject regression. |

Compensation is forward-moving: expire an abandoned claim; close a short task and
reallocate/release only its remainder; quarantine damaged units; unpack before seal;
cancel an unused label when supported. After confirmed hand-off, cancellation becomes
carrier intercept or reverse logistics, never deletion of shipped history. A failed
compensation is itself durable retryable work with an owner and exception state.

### Concrete scenario walkthrough

1. Order `O-81` requests `SKU-A x7` and `SKU-B x2`. Reservation `R-81` permits
   `WH-1: A5/B2` and `WH-2: A2`; key `create-O-81` creates one fulfilment order.
2. The committed plan creates three traceable tasks. Workers `W-7` and `W-8` race to
   claim the `WH-1/A5` task. A versioned claim lets `W-7` win with token `claim-3`;
   `W-8` cannot scan it.
3. `W-7` scans four units with `scan-900`. Replaying the same scan changes nothing;
   reusing `scan-900` for five units is an idempotency conflict. A short-pick command
   records the missing unit and closes the task.
4. Inventory approves that one-unit remainder at `WH-2`, producing a new task there.
   Physical truth stays `WH-1 picked A4`; `WH-2` now has work for `A3` in total.
5. Package `P-1` atomically consumes `WH-1`'s `A4/B2` picked balance. A concurrent
   attempt to pack another `A1` from `WH-1` fails because no unpacked picked balance
   remains. `P-1` is weighed and sealed with an immutable manifest.
6. Carrier request `ship-P-1-v1` times out. Its attempt is `UNKNOWN`; sealed `P-1`
   cannot spawn a new shipment identity. A query confirms tracking `T-1`; a later
   duplicate callback is a no-op. The order is `PARTIALLY_SHIPPED` until the `WH-2`
   package is confirmed, then projects `SHIPPED`. Versioned outbox facts let Order
   Management build the same progression without owning warehouse state.

### Expected solution and design-principle reasoning

A strong exercise-scale solution uses `FulfilmentOrder`/line balances as the local
consistency boundary, with explicit `PickTask`, `Package`, and `ShipmentAttempt`
lifecycles. An application service coordinates repositories and the inventory,
carrier, policy, and event ports. At higher contention, tasks and packages can become
separate aggregates while conditional line-consumption records preserve the same
invariants.

- **SRP:** tasks own claim/pick transitions, packages own content/seal transitions,
  line balances protect quantity conservation, and workflow services orchestrate
  across them.
- **OCP and DIP:** allocation/assignment policies and carrier integrations implement
  domain-owned interfaces, so new policies/providers do not change scan or quantity
  rules.
- **ISP:** separate inventory, carrier, policy, and publishing ports avoid a “WMS
  manager” interface that every adapter must partially fake.
- **LSP:** every allocation strategy must return a complete, valid proposal from the
  same inputs, and every carrier adapter must preserve stable correlation and unknown
  semantics. The application layer still validates untrusted strategy output.
- **Strategy** is justified for genuinely variable allocation, wave, or assignment
  policy; **Adapter** normalizes carrier protocols. Guarded domain transitions are
  simpler than a class-per-state hierarchy while behaviour is compact. Repository /
  unit of work plus **transactional outbox** handles atomic local change and events;
  a **saga** coordinates reallocation and carrier compensation without two-phase
  commit.
- Reject public setters on stateful records, one global service lock, read-then-write
  counters, direct inventory mutation, a single order status as all truth, and a
  distributed transaction with inventory or carrier. Event sourcing is an optional
  traceability choice, not a prerequisite for an append-only audit.

Testability comes from a pure deterministic allocator; fake inventory/carrier ports;
injected clock, IDs, and lease policy; transition-table and property tests for
quantity conservation; barrier-based claim/pack races; scan-fingerprint tests;
adapter contract tests; and fault injection around outbox and carrier call/commit
windows.

### Senior and Staff expectations

| Decision | Strong Senior baseline | Staff-level evolution trigger and response |
| --- | --- | --- |
| Consistency boundary | Versioned order/line transaction; no cross-order lock | Split task/package aggregates under contention; use conditional balance-consumption records and an async order projection |
| Work dispatch | Indexed available tasks plus atomic leased claim | Partition queues by warehouse/zone, add fairness/priority and lease-recovery SLOs without weakening the claim epoch |
| Allocation | Deterministic eligible-source strategy | Wave/batch planning, capacity/labour constraints, explainable optimization, and replan events with policy version |
| Traceability | SKU, bin, task, worker, package, and scan history | Lot/serial/expiry/FEFO and chain-of-custody identities; preserve original physical provenance |
| Multi-warehouse data | One service with warehouse-scoped writes | Partition by warehouse, accept asynchronous cross-warehouse order projections, and design regional failure isolation |
| Carrier integration | Stable hand-off IDs, query, callback, and outbox | Label/handoff reconciliation, carrier SLO routing, manifest close, discrepancy tooling, and controlled adapter rollouts |
| Contracts/operations | Versioned facts and exception reason | Schema compatibility, replay/backfill, operator repair commands, audit retention, aging dashboards, and explicit team ownership |

Staff signal is a credible path from local correctness to high-throughput warehouse
autonomy and cross-team operations—not prematurely distributing every object.

### Interviewer follow-ups with concise strong answers

| Question | Strong answer |
| --- | --- |
| Why not make order status the aggregate FSM? | Split children progress independently. Enforce task/package lifecycles and conserved quantities, then derive an order projection such as partially shipped or exception. |
| How is a task claim atomic across instances? | Use a database conditional update on state/version/lease expiry that also creates a claim epoch; process-local locks are insufficient. |
| What if a worker returns after its lease expired? | Its old claim token is stale. Already committed scans remain; only unfinished quantity belongs to the new claim epoch. |
| Same scan ID with a different barcode or quantity? | Return a conflict and audit/quarantine it. Treating it as a duplicate would conceal device or operator corruption. |
| How is a short pick different from cancellation? | A short pick records observed physical absence after work starts; cancellation is business intent. Persist the pick, then release/reallocate only the unpicked remainder through Inventory. |
| How do two packages avoid consuming the same picks? | Atomically create unique consumption records or conditionally decrement picked-but-unpacked balance in the same transaction as package contents. |
| What if cancellation races the final scan? | Both conditionally update the affected line/allocation version. One wins; the loser reloads and may cancel only the quantity that remains physically eligible. |
| Why is carrier timeout an unknown outcome? | The carrier may have accepted the package/request after our deadline. Reusing a stable ID and querying prevents two labels or logical shipments. |
| Can events be exactly once? | Publish at least once from a transactional outbox. Stable event IDs and aggregate versions make consumer application idempotent and monotonic. |
| What if the outbox publisher is down? | Warehouse state remains committed; a retrying publisher drains pending rows. Backlog age is an operational SLO, not a reason to roll back physical work. |
| How would lots, serials, or FEFO fit? | Extend allocation and scan identity with lot/serial/expiry; FEFO is policy, while uniqueness and quantity/provenance invariants remain domain rules. |
| Can one package contain multiple orders? | Assume no for the core traceability boundary. Waves/totes may combine temporary work; cross-order final packaging needs an explicit container hierarchy and different invariants. |
| How do you reduce a hot order row? | Promote task/package to aggregates, atomically consume line balance by conditional records, partition writes by warehouse, and build the cross-split order view asynchronously. |
| What should upstream events say? | Publish facts—task short-picked, package shipped, fulfilment exception—with stable IDs, source order/line, quantities, location, version, and time; upstream owns its projection. |
| When is a State pattern justified? | When states have substantial, varying behaviour and dependencies. For small transitions plus quantity guards, explicit domain methods/table are easier to audit and test. |
| What is the highest-value test? | Concurrent and fault-injected command sequences proving no unit is picked, packed, or shipped twice and no stale claim/callback regresses physical truth. |

### Red flags

- Conflates reservation with physical stock, or lets Fulfilment and Inventory both
  mutate the same availability record.
- Represents a split order with one mutable status and no per-line/task/package
  quantities or provenance.
- Claims tasks with `if (available) setClaimed`, uses a process-only lock, or allows
  scans without worker and claim-epoch validation.
- Deduplicates a scan by barcode/task rather than stable scan ID plus fingerprint, or
  updates counters outside the dedupe transaction.
- Packs from allocated rather than picked quantity, permits cross-warehouse contents,
  or checks remaining balance before rather than during the atomic write.
- Treats carrier timeout as failure and creates a new shipment/label, or deletes
  shipped state to “cancel.”
- Publishes before commit with no outbox, assumes ordered/exactly-once events, or lets
  upstream messages overwrite warehouse truth.
- Uses Strategy/State/Factory for every noun but cannot name the varying policy,
  invariant owner, or recovery test.

### Interview variations and expected solutions

These variations synthesize the distinct source prompts and implementation ideas at
`low-level-design-primer@49fe9f2`, `awesome-low-level-design@fc26e40`, and
`kumaransg/LLD@1698cc6`. Unlicensed material is summarized; neighbouring logistics
fragments are inputs to scope and critique, not warehouse reference solutions.

#### Variation 1 — Warehouse management and physical execution

- **Candidate prompt / scope delta:** Expand the primer's terse “Warehouse Management
  System” into the canonical receive-allocation, pick, pack, ship, exception, and
  cancellation scope. Inventory reservation remains an upstream boundary.
- **Expected Senior solution:** Model line balances, allocations, leased pick tasks,
  stable scan IDs/fingerprints, warehouse-bound packages, shipment attempts, and an
  outbox. Enforce quantity conservation and atomic claim/pack consumption; reconcile
  ambiguous carrier hand-offs.
- **Staff-level extension / trade-offs:** Define aggregate partitioning by warehouse,
  waves and capacity policy, lot/serial/FEFO traceability, event compatibility,
  operator repair, exception-aging SLOs, and the consistency trade-off of an
  asynchronous cross-warehouse order projection.
- **Canonical runnable solution:** Applies directly as the executable baseline. A
  production answer replaces the single process transaction lock/maps with durable
  versioned rows or conditional writes while keeping task, scan, quantity, package,
  idempotency, and outbox invariants intact.

Provenance: [`References/PRIMER_QUESTION_INDEX.md`](../../References/PRIMER_QUESTION_INDEX.md)
maps [`References/low-level-design-primer/questions.md`](../../References/low-level-design-primer/questions.md)
to this problem; see the pinned [L135 prompt](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L135).

#### Variation 2 — End-to-end logistics system

- **Candidate prompt / scope delta:** Broaden the problem to order intake, priority,
  packages/consignments, vehicle or carrier assignment, tracking, delivery, payment,
  and cancellation. The candidate must separate warehouse execution from transport
  instead of building a `LogisticsSystem` god object.
- **Expected Senior solution:** Give `Consignment`/`Shipment`, `VehicleAssignment`, and
  tracking events their own lifecycles; use a capability/assignment policy; make
  assignment atomic and idempotent; and connect the warehouse's sealed-package
  hand-off through a port. Commercial order and payment remain separate owners.
- **Staff-level extension / trade-offs:** Add network/route planning, regional capacity,
  streaming location and ETA projections, regulatory/carrier constraints, rebalancing,
  and sagas for cancellation/intercept. Discuss bounded contexts and why tracking may
  favor high-volume append-only events over the warehouse transaction model.
- **Canonical runnable solution:** Pick/pack, manifest, outbox, idempotency, and
  hand-off logic apply as the warehouse subdomain. Adapt by adding transport
  aggregates/services; do not stretch the canonical `Shipment` record to own vehicle
  availability, live tracking, delivery, and payment.

Provenance: the primer's [`questions.md`](../../References/low-level-design-primer/questions.md)
has the pinned [L112 logistics prompt](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L112),
while [`solutions.md`](../../References/low-level-design-primer/solutions.md)
([pinned L20](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/solutions.md#L20))
is **outbound-link only**. Kumar's
[`SystemDesign/LogisticsDesign`](../../References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LogisticsDesign/)
([pinned](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/LogisticsDesign))
is a broad model **fragment**; its
[`Leetcode/G4G` path](../../References/kumaransg-LLD/Low_level_Design_Problems/Leetcode/G4G/src/SystemDesign/LogisticsDesign/)
is an **exact duplicate** recorded in
[`References/VARIATION_INDEX.md`](../../References/VARIATION_INDEX.md#exact-duplicate-paths).

#### Variation 3 — Last-mile agent scoring and assignment

- **Candidate prompt / scope delta:** Assign a delivery executive to an order using
  availability, distance, order wait, and worker fairness. This is last-mile dispatch,
  not bin picking or package construction.
- **Expected Senior solution:** Keep scoring as a pure Strategy over immutable
  snapshots, but place the actual assignment behind a conditional availability/
  version write or lease so two orders cannot win one agent. Model assignment
  lifecycle, idempotent retries, units/time, and requeue after rejection/expiry.
- **Staff-level extension / trade-offs:** Address streaming/stale location, geospatial
  partitioning, supply-demand balancing, fairness and incentives, batching, policy
  explainability/versioning, and the latency-versus-global-optimality trade-off.
- **Canonical runnable solution:** Atomic pick-task claim, stable command identity,
  outbox, and policy-port ideas transfer. Replace warehouse task/scan/package
  aggregates with agent/assignment/trip lifecycles; carrier hand-off and line-quantity
  invariants do not transfer.

Provenance: Kumar's
[`swiggyInterview1`](../../References/kumaransg-LLD/Low_level_Design_Problems/swiggyInterview1/)
([pinned](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/swiggyInterview1))
is indexed as a distinct last-mile variation in
[`References/VARIATION_INDEX.md`](../../References/VARIATION_INDEX.md#warehouse_fulfilment_domain).
The awesome clone's
[`fooddeliveryservice/strategy`](../../References/awesome-low-level-design/solutions/java/src/fooddeliveryservice/strategy/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/fooddeliveryservice/strategy))
is a simpler nearest-agent **fragment**; its language ports are collapsed as
conceptual **duplicates**.

#### Variation 4 — Shipping cost and rate policy

- **Candidate prompt / scope delta:** Calculate a shipping quote using flat, weight,
  distance, or third-party rules. This is a pricing/policy slice adjacent to
  fulfilment, not evidence that a package shipped.
- **Expected Senior solution:** Use exact `Money`, explicit units, an immutable
  `ShipmentQuote` with inputs/policy version/expiry, a Strategy for local algorithms,
  and an Adapter for a remote rate provider. Keep quoting free of package state
  mutation and define deterministic tie/error behaviour.
- **Staff-level extension / trade-offs:** Add concurrent carrier rate shopping,
  service-level/capacity constraints, cache and staleness policy, explainability,
  quote acceptance, surcharge audit, and resilience when a carrier quote is unknown
  or expires.
- **Canonical runnable solution:** A sealed package manifest can supply weight,
  dimensions, origin, and destination, and the port/Strategy reasoning transfers.
  Pricing belongs before carrier selection and outside the canonical quantity/task
  invariants; add quote identity rather than overloading `Shipment`.

Provenance: the Java
[`References/awesome-low-level-design/design-patterns/java/strategy/`](../../References/awesome-low-level-design/design-patterns/java/strategy/)
is available at the pinned
[shipping-strategy tree](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/java/strategy).
The C++, C#, and other ports repeat the same teaching example and are collapsed as
language **duplicates**, not independent warehouse solutions.

#### Variation 5 — Commerce shipment state and tracking records

- **Candidate prompt / scope delta:** Move a placed commerce order through shipped,
  delivered, or cancelled and retain shipment logs. Add split shipments to expose the
  limitation of a single order-state object.
- **Expected Senior solution:** Make Shipment/Package children carry their own
  quantities and carrier references, enforce cancellation eligibility per quantity,
  append tracking facts, and derive the commercial order projection. “Shipped” must
  follow confirmed hand-off, not a direct setter.
- **Staff-level extension / trade-offs:** Define multi-carrier tracking ingestion,
  partial delivery, loss/damage, SLA projections, returns/intercepts, event ordering,
  and ownership of customer-visible status versus physical facts.
- **Canonical runnable solution:** Package contents, split shipments, outbox, and
  terminal shipped quantity apply through carrier hand-off. Adapt with a transport /
  tracking boundary for in-transit and delivered states; replace coarse State
  fragments with child lifecycle plus derived order status.

Provenance: awesome's
[`onlineshoppingservice/state`](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/state/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/onlineshoppingservice/state))
and Kumar's
[`oops/amazon/Shipment.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/Shipment.java)
plus [`ShipmentLog.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/ShipmentLog.java)
([pinned tree](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon))
are neighbouring commerce **fragments**, not pick/pack solutions. The latter remains
indexed under e-commerce in
[`References/VARIATION_INDEX.md`](../../References/VARIATION_INDEX.md#e_commerce), so
it is not counted as another canonical warehouse implementation.

### Scoring rubric

Score each dimension from 0 to 4: `0` absent/unsafe, `2` workable with important
gaps, `4` precise and pressure-tested.

| Dimension | 4-point evidence |
| --- | --- |
| Domain model and invariants | Independent task/package lifecycles, conserved line quantities, traceable provenance, derived split-order status |
| Contracts and ownership | Clear Order/Inventory/Fulfilment/Carrier boundaries; usable commands, tokens, versions, ports, and fact events |
| Concurrency and recovery | Atomic leased claims, scan fingerprinting, safe package consumption, short-pick/cancel races, unknown hand-off reconciliation, outbox |
| Design reasoning and evolution | SOLID/pattern choices tied to variation; rejected unsafe alternatives; credible aggregate partitioning and warehouse-scale path |
| Communication and testing | Drives assumptions and scenario; explains physical truth clearly; proposes deterministic, race, property, contract, and fault tests |

Interpretation: `17-20` strong Senior with Staff evidence, `13-16` Senior-ready,
`9-12` mixed, and `0-8` below bar. A Staff recommendation additionally requires a
`4` in concurrency/recovery and design evolution plus cross-team/operability
reasoning. Any design that can double-pick, double-pack, or double-ship is a safety
veto regardless of total score.

## Functional Requirements

1. Create an idempotent fulfilment order from a confirmed order request.
2. Allocate fulfilment lines to one or more warehouses and shipments.
3. Create pick tasks containing SKU, quantity, source bin, and destination.
4. Assign or atomically claim work so two workers cannot complete the same task.
5. Validate item, quantity, bin, and package scans.
6. Pack picked items into one or more packages.
7. Mark packages shipped with carrier and tracking references.
8. Handle short picks, damaged stock, reallocation, and eligible cancellation.
9. Publish meaningful status changes back to the order-management boundary.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `FulfilmentOrder` | Aggregate for warehouse execution of ordered lines |
| `FulfilmentLine` | Requested, allocated, picked, packed, and shipped quantities |
| `Warehouse` / `Bin` | Physical location model |
| `PickTask` | Claimable unit of picking work |
| `Package` | Packed contents, weight, and shipping status |
| `Shipment` | Carrier hand-off and tracking details |
| `Worker` | Actor assigned to warehouse tasks |
| `FulfilmentEvent` | Auditable domain change for upstream consumers |

## Business Rules and State Transitions

- Define valid fulfilment states, for example `RECEIVED`, `ALLOCATED`, `PICKING`,
  `PICKED`, `PACKED`, `SHIPPED`, `CANCELLED`, and `EXCEPTION`.
- A line cannot be picked, packed, or shipped beyond its allocated quantity.
- A package may contain only picked, unpacked quantities from its fulfilment order.
- Shipping is terminal for the shipped quantity; cancellation rules differ before
  and after picking or packing.
- Every scan is validated against the current task and recorded once.
- Short-pick handling must retain a clear reason and either reallocate or escalate.

## Concurrency and Failure Handling

- Claiming a pick task must be atomic.
- Duplicate scans and repeated status messages must be idempotent.
- Concurrent package operations cannot pack the same item quantity twice.
- A failed carrier hand-off must leave the package retryable without creating two
  logical shipments.
- Upstream status publication may be retried without duplicating a state transition.

## Demonstration Scenarios

1. Complete a normal allocate, pick, pack, and ship workflow.
2. Split one order across two warehouses and shipments.
3. Let two workers compete to claim the same task.
4. Record a short pick and reallocate the remaining quantity.
5. Cancel eligible work before picking and reject cancellation after shipping.
6. Retry a duplicate scan and carrier response safely.

## Extensions

- Wave and batch picking
- FIFO or FEFO allocation policies
- Packing optimisation and label generation
- Returns and reverse logistics

## Related Problems

- [Order Management System](../order_processing_system/README.md)
- [Inventory Reservation Service](../inventory_reservation_service/README.md)
- [Product Catalog Service](../product_catalog_service/README.md)

---

## Python 3 Reference Solution

- [`python/solution.py`](python/solution.py) is the standard-library core a candidate
  can finish and defend in one hour: a pure deterministic split allocator, guarded
  line/task/package states, atomic claim and quantity changes, fingerprinted scan,
  pack, and ship commands, plus short-pick recovery.
- [`python/test_solution.py`](python/test_solution.py) has five decisive tests for
  deterministic allocation, a twelve-worker one-winner claim race, duplicate and
  conflicting scans, package quantity conservation, idempotent shipping, and
  replacement work after a short pick.

### One-hour Python coding scope

Implement live: demand/availability records, `SplitFirstFitAllocator.plan`, line
quantity conservation, claimable pick tasks, and create/claim/scan commands under an
`RLock` that represents one atomic database transaction. Add stable command
fingerprints and tests for a split allocation, a one-winner claim race, and a
duplicate scan. If time remains, add package balance consumption, seal/confirmed-ship
transitions, and short-pick replacement work.

Explain rather than type: HTTP/scanner endpoints, ORM schemas, database conditional
writes and expiring claim leases, durable idempotency tables, transactional outbox
publishing, multi-instance/distributed storage, warehouse queues, carrier adapters,
the `PENDING -> CONFIRMED | REJECTED | UNKNOWN` hand-off attempt and reconciliation
workflow, metrics, tracing, alerting, dashboards, and operator repair tooling.

Run with Python 3.9+ and only the standard library:

```bash
cd Problems/warehouse_fulfilment_domain/python
python3 test_solution.py
```

## Java 17 Reference Solution

### Implementation Status

| Capability | Status | Implementation |
| --- | --- | --- |
| Idempotent fulfilment creation | Complete | Unique external-order identity plus command fingerprint/reference records |
| Split warehouse/bin allocation | Complete | Pure `AllocationStrategy` and deterministic `SplitFirstFitAllocator` |
| Explicit order, task, package, shipment states | Complete | Guarded immutable model transitions |
| Atomic task claims and scarce quantity updates | Complete | One fair transaction lock covers check-and-replace operations |
| Validated, deduplicated scans | Complete | Stable scan IDs; worker, warehouse, bin, SKU, package, and quantity checks |
| Partial/short pick and reallocation | Complete | Partial scan is retained; short allocation is removed and replaced or escalated |
| Package contents and split shipments | Complete | Warehouse-bound package contents, seal weight, carrier, and tracking snapshots |
| Cancellation rules | Complete | Unpicked work may cancel; physical picks and shipped work reject cancellation |
| Status integration and immutable audit | Complete | Retryable in-memory outbox plus append-only aggregate audit snapshots |
| Runnable demonstration | Complete | Split flow, claim race, failures, duplicates, short pick, shipment, and cancellation |

### Architecture and Design Choices

- `FulfilmentOrder` is an immutable aggregate summary. `FulfilmentLine` enforces
  `shipped <= packed <= picked <= allocated` and ensures allocated plus cancelled
  quantity never exceeds demand. State transitions reject backward or terminal
  changes.
- `PickTask` is an immutable claimable unit containing source warehouse/bin, SKU,
  quantity, and destination. A fair `ReentrantLock` makes claim, scan, line-counter,
  package, shipment, idempotency, and outbox changes atomic in this in-memory
  implementation. Two workers therefore cannot both claim or consume one task.
- `AllocationStrategy` is pure: it sees approved availability snapshots and must
  return a complete valid allocation without mutation. The supplied first-fit policy
  sorts locations and can split a single order line across warehouses and bins. The
  service independently validates line totals and source capacity before creating
  any tasks.
- A pick scan ID is a deduplication key and its fingerprint includes task, worker,
  source, SKU, and quantity. A short pick closes the original task, retains already
  scanned units, removes only its unpicked allocation, and creates replacement tasks.
  Failed reallocation moves the order to `EXCEPTION` with the reason in its audit.
- Packages are opened for one warehouse. Package scans cannot exceed picked,
  unpacked units at that warehouse, so split allocations remain physically
  traceable. Packages must be non-empty and weighed before sealing. Each carrier
  confirmation creates one immutable `Shipment` with carrier and tracking data.
- Carrier calls receive stable idempotency keys. A failed hand-off leaves the
  package `SEALED`; retrying cannot create two logical shipments. Status changes use
  immutable `FulfilmentEvent` records in a retryable outbox, and publication failure
  never rolls warehouse state back.
- For a multi-instance production service, replace the process lock/maps with
  versioned rows or conditional writes and persist command records plus the outbox
  in the same transaction. The domain transitions, scan identities, and port
  contracts remain unchanged.

### Source Structure

```text
com/example/lld/warehouse_fulfilment_domain/
├── model/
│   ├── FulfilmentOrder.java        # aggregate state, IDs, immutable audit
│   ├── FulfilmentLine.java         # allocated/picked/packed/shipped counters
│   ├── FulfilmentStatus.java
│   ├── PickTask.java               # AVAILABLE/CLAIMED/terminal task states
│   ├── FulfilmentPackage.java      # OPEN/SEALED/SHIPPED/CANCELLED
│   ├── PackageContent.java
│   ├── Shipment.java
│   ├── Allocation.java
│   ├── OrderLineDemand.java
│   ├── WarehouseAvailability.java
│   ├── Destination.java
│   ├── AuditEntry.java
│   └── FulfilmentEvent.java
├── service/
│   ├── WarehouseFulfilmentService.java
│   └── SplitFirstFitAllocator.java
├── port/
│   ├── AllocationStrategy.java
│   ├── CarrierPort.java
│   ├── StatusPublisher.java
│   └── IdGenerator.java
├── exception/
│   ├── WarehouseDomainException.java
│   ├── AllocationException.java
│   ├── InvalidStateException.java
│   ├── InvalidScanException.java
│   ├── TaskAlreadyClaimedException.java
│   ├── IdempotencyConflictException.java
│   └── NotFoundException.java
└── demo/
    └── Main.java
```

### Compile and Run

From the repository root:

```bash
WAREHOUSE_OUT="$(mktemp -d /private/tmp/warehouse-fulfilment.XXXXXX)"
find Problems/warehouse_fulfilment_domain -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -d "$WAREHOUSE_OUT"
java -cp "$WAREHOUSE_OUT" com.example.lld.warehouse_fulfilment_domain.demo.Main
```

Key output includes:

```text
Warehouse Fulfilment Domain demo passed
  claim winners: 1/2
  shipments: 3
```
