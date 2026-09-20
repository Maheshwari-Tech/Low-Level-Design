# Order Management System

## Problem Description

Design an order management system for an e-commerce platform. The system owns an
order from creation until completion or cancellation and coordinates with inventory,
payment, promotion, and warehouse services through explicit interfaces.

The design must keep the commercial order lifecycle separate from the internal
lifecycle of payment and fulfilment. A failure in one dependency must not leave the
order in an unexplained or contradictory state.

## 60-Minute Senior/Staff Interview Guide

This guide is the interview frame. The requirements and Java 17 reference solution
later in this page are the detailed specification and one executable realization;
the candidate should not spend the hour restating them.

### Candidate-facing question

> Design the domain and application layer for an e-commerce order service. An order
> contains immutable commercial snapshots and coordinates inventory, payment,
> promotion, and fulfilment through explicit interfaces. Support create, confirm,
> line cancellation, and fulfilment updates. Make retries safe, prevent concurrent
> commands from applying the same quantity twice, and explain what the caller sees
> when a dependency or database outcome is unknown. Start in one process, then show
> how the same boundaries survive a multi-instance deployment. Focus on objects,
> command semantics, invariants, and executable failure reasoning—not UI, service
> discovery, or a generic microservice diagram.

### Minute-by-minute plan

| Time | Candidate objective | Interviewer signal |
| --- | --- | --- |
| 0–5 | Restate the commercial boundary and name assumptions | Separates order ownership from stock, money, and warehouse ownership |
| 5–12 | Ask clarifying questions; freeze confirmation and cancellation semantics | Finds ambiguity before drawing classes |
| 12–20 | Model the aggregate, value objects, independent statuses, and invariants | Uses behavior-rich methods and quantity counters, not public setters |
| 20–29 | Define commands, results, ports, repository, and ownership | Makes replay, conflict, rejection, and pending outcomes observable |
| 29–39 | Walk through create and confirm, including compensation | Places each validation and side effect deliberately |
| 39–48 | Resolve retries, command races, timeouts, and crash points | Reasons about linearization and unknown outcomes rather than saying “rollback” |
| 48–54 | Cover line cancellation, fulfilment callbacks, and tests | Preserves quantities under duplicate/out-of-order input |
| 54–58 | Evolve the in-memory design to durable multi-instance execution | Introduces storage and messaging only where a failure requires them |
| 58–60 | Summarize trade-offs and the strongest remaining risk | Communicates a coherent, scoped design |

### Clarifying questions and strong assumptions

| Ask | Strong default answer for this interview |
| --- | --- |
| What does “confirm” guarantee? | All requested inventory is reserved and the configured payment step has succeeded. This exercise uses capture; authorization/capture can later be separate payment phases. Confirmation is all-or-none across order lines. |
| Who computes price, tax, and promotion? | The order receives validated price/currency snapshots and records the promotion result. Catalog and tax computation are outside the aggregate; a confirmed snapshot never silently reprices. |
| May one order contain multiple currencies or sellers? | One currency and one checkout boundary. Multi-seller splitting creates child orders and is an extension. |
| What cancellation is allowed? | In this exercise, only confirmed, unfulfilled quantity may be cancelled. A full cancellation cancels every still-open line. Refund and inventory-release requests use stable keys; returns are separate. |
| Is payment or fulfilment status the order status? | No. Commercial, payment, fulfilment, and workflow-operation state are separate dimensions with explicit derivation rules. |
| Are downstream calls transactional with the order store? | No distributed ACID is assumed. Each dependency supports an idempotency key and lookup-by-key; coordination is a saga/process manager. |
| What does idempotent mean here? | Same operation scope + key + canonical request returns the original result. Reusing that key with different input is a conflict, and a retry never invents a new downstream key. |
| What consistency is required? | Commands for one order are serialized/optimistically versioned. Reads may use a lagging projection if the response exposes its version; command results read from the write model. |
| What scale assumption matters to LLD? | `orderId` is the natural write key. No command needs a global order lock or an all-orders transaction. |
| How are unknown remote outcomes represented? | As a durable nonterminal operation such as `PENDING_RECONCILIATION`, never as a guessed failure or a second charge/reservation. |

### Interview scope

**Required behavior:** create an order snapshot, confirm it, cancel eligible
quantities, accept fulfilment progress, return current state/history, and make every
command replay-safe. Promotion is invoked at a documented point; inventory,
payment, and fulfilment are ports. The detailed list below remains the source of
truth.

**Quality requirements:** no double charge/reservation/fulfilment from retries;
no over-fulfilment or cancellation of shipped quantity; exact decimal money;
per-order command serialization across instances; durable audit and idempotency
records; deterministic recovery after process death; dependency timeouts and
correlation IDs; and tests that do not use wall-clock time or real networks.
Availability is subordinate to commercial safety when an outcome is unknown.

**Out of scope:** catalog/search, tax engines, authentication, physical stock
counting, carrier routing, returns/exchanges, chargebacks, notification rendering,
global analytics, and a full event-platform design. Name their ports or events only
when they change an order decision.

### Core ownership boundaries

| Owner | Owns | Must not decide |
| --- | --- | --- |
| `Order` aggregate | Line snapshots, totals, cancellable/fulfilled counters, commercial status, version, and domain history | Whether remote stock exists or a card charge succeeded |
| Order application service | Command transaction, idempotency receipt, aggregate load/save, workflow step selection, and port calls | Internal rules of inventory, payment, promotion, or warehouse systems |
| Confirmation process manager | Durable progress, stable downstream keys, compensation, retry, and reconciliation | Order quantity invariants |
| Inventory service | Availability, allocation, reservation lifecycle, and reservation ID | Order price or payment success |
| Payment service | Authorization/capture/refund lifecycle and payment transaction ID | Order fulfilment or stock allocation |
| Promotion service | Eligibility and deterministic discount breakdown for its input/version | Mutation of an accepted order snapshot |
| Fulfilment service | Shipment units and warehouse execution | Commercial cancellation eligibility |
| Read model | Searchable customer/order views derived from committed facts | Command acceptance |

The synchronous reference implementation can combine the application service and
process manager. The boundary, durable identifiers, and result semantics should
still be visible in the design.

### Commands, APIs, and response semantics

| Command | Essential input | Successful result |
| --- | --- | --- |
| `createOrder` / `POST /orders` | Customer, immutable line/price snapshots, address, `Idempotency-Key` | `201 Created`; order snapshot, version, and command ID. Exact replay may return `200` with `replayed: true`. |
| `confirmOrder` / `POST /orders/{id}/confirmation` | `Idempotency-Key`, optional `expectedVersion` | `200 CONFIRMED`, or `202 PENDING_RECONCILIATION` with an operation URL when a step is in doubt |
| `cancelQuantity` / `POST /orders/{id}/cancellations` | Line quantities, reason, key, optional version | Updated snapshot plus compensation status; never reports cancelled before the aggregate accepts the quantity |
| `recordFulfilment` / `POST /orders/{id}/fulfilments` | Warehouse event/command ID, per-line delta quantity, key | Updated line counters; duplicates return the previously recorded version |
| `getOrder` / `GET /orders/{id}` | Order ID | Snapshot, independent payment/fulfilment summaries, version, and links to history/active operation |

Use a typed result such as `Applied`, `Replayed`, `Pending`, or `Rejected`; do not
encode all business outcomes as exceptions. Suggested HTTP mapping: `404` unknown
order, `409` stale version/invalid transition/idempotency-key conflict, `422`
malformed or permanently invalid business input, and `503` only when no durable
command was accepted. A definitive decline is different from a timeout. Persist the
canonical request fingerprint and response before declaring a command complete.

### State machine and invariants

Keep the small commercial state machine independent from workflow progress:

```text
PENDING ──confirm──> CONFIRMED ──fulfil some──> PARTIALLY_FULFILLED
                         │                              │
                  cancel every unit          fulfil/cancel every open unit
                         │                              │
                         v                              v
                     CANCELLED                      FULFILLED
```

Partial line cancellation updates counters without requiring another order-level
state. `FULFILLED` means every non-cancelled unit is fulfilled; an order with any
fulfilled quantity is not later relabeled `CANCELLED`. Line counters, rather than
an enum for every combination, answer partial cases.
Separately, a confirmation operation may be `IN_PROGRESS`,
`PENDING_RECONCILIATION`, `COMPENSATING`, `SUCCEEDED`, or `FAILED`; payment and
fulfilment have their own states.

The aggregate must maintain:

- `orderedQty > 0`, `fulfilledQty >= 0`, `cancelledQty >= 0`, and
  `openQty = orderedQty - fulfilledQty - cancelledQty >= 0` for each line.
- After order confirmation, a fulfilment or cancellation delta cannot exceed that
  line's `openQty`; pending lines are not mutated by those commands.
- Unit price, currency, applied per-unit discount, SKU snapshot, and delivery
  address version are immutable after confirmation. All money arithmetic preserves
  currency and its exact scale.
- The payable total is derived from accepted line snapshots and quantities; it is
  never accepted as an unrelated mutable field.
- Exactly one aggregate method validates and applies each transition. Version and
  chronological history advance in the same local transaction as the state change.
- A downstream business effect uses one stable key derived from order + workflow
  step (for example `O-42:confirm:payment`), not a fresh key per network attempt.
- A terminal commercial state cannot be reopened. Returns or corrections create a
  new explicit workflow rather than rewriting history.

### Expected solution and design-principle reasoning

**Expected shape.** `Order` is an immutable or tightly encapsulated aggregate with
methods such as `confirm`, `cancelQuantity`, and `recordFulfilment`. Value objects
(`Money`, `Address`, IDs) reject invalid construction. An `OrderApplicationService`
handles command receipts and repository transactions; a confirmation process
manager invokes narrow ports and records step outcomes. Adapters implement those
ports. Read projections and event publication sit outside the aggregate. See
**Architecture and Design Choices** below for how the reference solution applies
this shape in memory.

| Principle/pattern | Why it fits here | Boundary that prevents over-design |
| --- | --- | --- |
| SRP | Aggregate protects commercial invariants; application service coordinates a command; adapters translate remote protocols | Do not put HTTP retries, SQL, or payment SDK types in `Order` |
| OCP + Strategy | Promotion or cancellation policy can vary behind a small contract | A strategy is justified only for a genuine policy axis, not for every `if` |
| ISP | Inventory, payment, promotion, fulfilment, clock, and ID ports expose only operations this workflow needs | Avoid a generic `ExternalService` or CRUD repository god-interface |
| DIP | Domain/application code depends on ports; adapters depend inward | Stable domain types do not import frameworks or vendor payloads |
| LSP | Any adapter must preserve idempotency, definitive-vs-unknown outcome, money, and correlation contracts | A fake that always succeeds is not a valid substitute for failure tests |
| Aggregate + Value Object | One consistency boundary owns line quantities; exact immutable values remove invalid intermediate states | Do not make the whole checkout ecosystem one aggregate |
| Saga/process manager | Inventory and payment cannot share a transaction; progress and compensation must survive a crash | Compensation is a business action, not an exception-stack rollback |
| Repository + unit of work + outbox | Order, command receipt, history, and outgoing fact commit together in production | Do not hold a database transaction open across remote calls |

**Alternatives rejected:** a single mega-enum for the cross-product of order,
payment, and shipment states; mutable anemic entities with public status setters;
blind retry plus “exactly once” claims; two-phase commit across external services;
and a GoF State class per status while transitions are still small and data-driven.
A State pattern becomes worthwhile only when each state acquires substantial,
distinct behavior. Domain events are useful, but an in-process Observer does not
make publication durable; use an outbox when external delivery matters.

**Testability and extension:** inject clock, IDs, repositories, policies, and
scriptable ports; use immutable snapshots in assertions. Table-test every state
transition, property-test quantity/money invariants, contract-test adapter replay
semantics, and use barriers to force confirm/cancel and cancel/fulfil races. New
payment phases, split shipments, or returns add explicit states/workflows without
weakening existing aggregate methods.

### Concurrency, idempotency, and failure reasoning

**Linearization.** In memory, a per-order lock can cover load/check/apply/save. In a
multi-instance service, use a repository version (`UPDATE ... WHERE version = ?`) or
a short row lock. A unique `(tenant, operation, idempotencyKey)` record and its
request fingerprint live in the same local transaction. Confirm racing with cancel,
or fulfilment racing with cancel, therefore has one winner; the loser reloads and
gets a precise conflict or applies only the still-eligible quantity.

**Confirmation failure table:**

| Point | Correct durable interpretation and next action |
| --- | --- |
| Inventory rejects | Record definitive failure; keep order `PENDING`; no payment call |
| Inventory times out | Mark operation pending and query reservation by the same key; do not charge while stock outcome is unknown |
| Payment declines after reserve | Request idempotent release; keep order `PENDING`; record decline and compensation progress |
| Payment times out | Query payment by stable key. Keep/renew the hold according to policy; neither charge again with a new key nor release as if decline were known |
| Process dies after charge but before order save | On recovery, look up the charge by key, then commit confirmation and outbox; the command receipt prevents a second workflow |
| Release times out | Keep `COMPENSATING`, retry/reconcile, expose the leak age, and alert before reservation TTL/SLA is breached |
| Outbox delivery duplicates | Consumer inbox/event ID makes fulfilment or notification application idempotent |

Store definitive rejections as command results if the contract promises exact
replay; use a new key when the caller intentionally wants a fresh attempt after
conditions change. Retention must cover the business retry horizon. There is no
general “exactly once” delivery—there is at-least-once execution plus durable
deduplication and naturally idempotent state transitions.

### Concrete walkthrough

1. `createOrder(K-create, O-42)` snapshots two units of SKU-A and one of SKU-B at
   INR 500 and INR 300. The order and receipt commit at version 1; a retry returns
   that exact snapshot.
2. `confirmOrder(K-confirm)` creates workflow keys `O-42:inv:1` and
   `O-42:pay:1`. Inventory returns reservation R-9; payment captures P-7.
3. The process crashes after P-7 but before saving `CONFIRMED`. The client receives
   no answer—an unknown outcome, not a failed payment.
4. The retry finds the existing command receipt, queries P-7 by the same key, and
   commits `CONFIRMED`, history, and `OrderConfirmed` outbox at version 2. It does
   not reserve or charge again.
5. A warehouse update for one SKU-A unit and a cancellation for both SKU-A units
   race at version 2. Whichever commits first increments the version. The other
   reloads and may cancel only the one still-unfulfilled unit; the invariant rules
   out both over-fulfilment and over-cancellation.

### Senior and Staff expectations

| Concern | Strong Senior answer | Additional Staff-level answer |
| --- | --- | --- |
| Consistency | Aggregate methods plus per-order lock/OCC and durable idempotency | Defines write ownership by `orderId`, conflict metrics, retry budget, and hot-order handling |
| Workflow | Explicit reserve → charge → confirm with compensation | Durable process manager, reconciliation queue, stuck-step SLA, and operator repair commands |
| Messaging | Emits domain facts after commit | Transactional outbox/inbox, schema/version policy, replay, ordering key, and poison-event handling |
| Storage evolution | Replaces maps with repository transactions without changing domain methods | Separates write model/read projections, plans zero-downtime state migration and command-receipt retention |
| Availability | Returns pending when safety is uncertain | Names dependency budgets, circuit behavior, admission control, and the commercial cost of fail-open vs fail-closed |
| Broader scope | Adds returns/split shipments as explicit workflows | Identifies bounded-context ownership and avoids a central “order” model shared by every team |

### Interviewer follow-up questions and concise strong answers

| Question | Strong answer |
| --- | --- |
| Why reserve before charge? | It avoids charging an order known to be unavailable. Authorization-first can be valid when holds are expensive, but the chosen order and compensation must be explicit. |
| Can inventory and payment run in parallel? | Only if the latency win justifies more compensations and both outcomes are durably joined. It is not a free optimization. |
| Do we provide exactly-once confirmation? | We provide effectively-once business effects through stable keys, unique receipts, and guarded transitions; transport remains at least once. |
| Why not keep a database lock while calling payment? | It creates long transactions, pool exhaustion, and lock amplification. Persist workflow intent, release the transaction, call, then persist the observed outcome. |
| How do you distinguish decline from timeout? | Decline is a definitive result and may trigger release. Timeout is unknown and must be queried/reconciled by the same payment key. |
| What if the same idempotency key has a different body? | Return an idempotency conflict; returning the old result would hide a caller bug. |
| How is cancellation safe against fulfilment? | Both are aggregate commands over versioned line counters. One linearizes first; the other revalidates the remaining quantity. |
| Why not derive order state only from the latest event? | Events/history are valuable, but the command path needs a transactionally current aggregate snapshot or a correctly rebuilt stream with concurrency checks. |
| When would you add the State pattern? | When states contain substantial distinct behavior or dependencies. For a compact transition table, guarded domain methods are clearer. |
| How do projections expose freshness? | Include aggregate/event version and last-updated time; use the write result for read-your-write instead of pretending a lagging view is current. |
| How would multi-seller orders change the design? | A parent checkout coordinates independently owned seller orders. It does not expand one aggregate or transaction across sellers. |
| What is the first production alarm? | Age/count of commands in unknown or compensating states, split by dependency and step, with reservation-expiry and money-exposure thresholds. |

### Red flags

- Starts with services, queues, or tables before defining what confirmation means.
- Treats order, payment, and shipment as one status field or allows arbitrary status
  setters.
- Uses `double` for money or keeps live product references instead of snapshots.
- Says `ConcurrentHashMap`, `synchronized`, or a message broker alone prevents a
  multi-step race.
- Retries a timed-out charge/reservation with a new downstream key.
- Calls compensation a rollback, assumes a remote timeout means failure, or hides
  uncertainty behind `500`.
- Makes the order aggregate own catalog, stock, payment, and warehouse internals.
- Adds Strategy, State, Factory, Observer, and Singleton by name without a changing
  policy or correctness benefit.
- Has only happy-path unit tests and no forced crash/race scenario.

### Scoring rubric

| Area | Points | Senior bar | Staff-strength evidence |
| --- | ---: | --- | --- |
| Scope and clarification | 10 | Freezes confirm/cancel semantics and exclusions | Quantifies safety/availability trade-off and identifies bounded contexts |
| Domain model and invariants | 20 | Encapsulated aggregate, exact money, valid quantity/state transitions | Cleanly evolves partial fulfilment, returns, or child orders without weakening invariants |
| Commands and ownership | 15 | Precise inputs/results, ports, repository boundary | Versioned contracts, read freshness, operational repair surface |
| Concurrency and idempotency | 20 | One linearization strategy; fingerprinted durable keys | Multi-instance contention, retention, consumer dedupe, and retry-budget reasoning |
| Failure and recovery | 20 | Correct compensation and unknown-outcome handling | Durable process/reconciliation design with SLAs and crash-point analysis |
| Principles, tests, evolution | 15 | Justified SOLID/pattern choices and deterministic tests | Migration, observability, schema evolution, and explicit rejected alternatives |

`70–84` is a solid Senior solution; `85+` with evidence in at least three
Staff-strength columns is Staff-level. A solution that cannot prevent a duplicate
charge or contradictory line quantities should not pass regardless of total.

### Interview variations and expected solutions

These are alternative interview cuts synthesized from the three pinned local
clones. They summarize unlicensed material; they do not copy it. Exact and language
duplicates are intentionally one variation, and fragments are inputs to improve—not
reference answers. Each **Python code fit** note says what can be reused from the
small live-coding core and what belongs in the fuller Java or production design.

#### Variation 1 — Core order-management lifecycle

- **Candidate prompt/scope delta:** The source is only “design an order-management
  system,” so use the canonical prompt above: immutable order snapshots plus
  confirm, cancellation, fulfilment, idempotency, and dependency failure semantics.
- **Expected Senior solution:** A behavior-rich `Order` aggregate, exact `Money`,
  explicit transitions/line counters, narrow dependency ports, versioned commands,
  and stable downstream keys with compensation.
- **Staff extension/trade-offs:** Durable process-manager state, reconciliation of
  unknown outcomes, outbox/inbox delivery, read-model freshness, and operational
  repair without widening the aggregate.
- **Canonical runnable solution / Python code fit:** The Python core applies directly:
  immutable snapshots, guarded transitions, command replay, stable dependency keys,
  and compensation. Its locks/maps are the seam for durable storage and workflow.

Provenance: `References/low-level-design-primer/questions.md` and
`References/PRIMER_QUESTION_INDEX.md` ([local prompt](../../References/low-level-design-primer/questions.md),
[local index](../../References/PRIMER_QUESTION_INDEX.md)); [pinned prompt line 177
at `49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L177).
This is a **one-line prompt fragment**, not a supplied solution.

#### Variation 2 — Order slice inside a broad online-shopping system

- **Candidate prompt/scope delta:** Accept a cart, place an order, update inventory,
  process payment, and track shipment, but require the candidate to draw the order
  boundary instead of designing catalog, search, user profile, and UI in depth.
- **Expected Senior solution:** Snapshot cart lines into an order, keep payment and
  shipment state separate, request an atomic inventory operation, hide payment
  choice behind a port/strategy, and replace public status mutation with guarded
  domain methods.
- **Staff extension/trade-offs:** Split catalog, order, payment, and fulfilment
  ownership; use saga/outbox semantics rather than the cloned Singleton/Observer
  style; define API versioning and read projections.
- **Canonical runnable solution / Python code fit:** Reuse the Python aggregate,
  exact money, ports, compensation, and replay; add cart-to-order mapping at the
  edge. Shipment and the broader shopping surface remain design discussion.

Provenance: `References/awesome-low-level-design/problems/online-shopping-service.md`,
`References/awesome-low-level-design/class-diagrams/onlineshoppingservice-class-diagram.png`,
and the Java/Python/C++/C#/Go `onlineshoppingservice` directories under
`References/awesome-low-level-design/solutions/` ([local problem](../../References/awesome-low-level-design/problems/online-shopping-service.md),
[local solution root](../../References/awesome-low-level-design/solutions/),
[local Java representative](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/));
[pinned problem](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/problems/online-shopping-service.md)
and [pinned solution root at `fc26e40`](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions).
The five implementations are **language ports/repetitions**, collapsed here.

#### Variation 3 — Purchase, return, blacklist, and bestseller workflow

- **Candidate prompt/scope delta:** Add products, purchase one, return it, blacklist
  a user, and report overall/per-category bestsellers. This is a compact machine-
  coding prompt, not checkout coordination across remote services.
- **Expected Senior solution:** Separate catalog stock, an immutable purchase/return
  ledger, user eligibility policy, and bestseller query projection. Make purchase
  and return idempotent and serialize stock mutation; never erase purchases when a
  user is blacklisted.
- **Staff extension/trade-offs:** Publish purchase/return facts to a replayable
  analytics projection, define tie/window semantics, and make blacklist decisions
  auditable without coupling fraud policy to order history.
- **Canonical runnable solution / Python code fit:** Reuse immutable lines, guarded
  cancellation, and command receipts from Python. Add local stock/eligibility ports
  plus a purchase-return ledger and projection; do not rewrite fulfilled orders.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/`,
its exact duplicate `.../Order-Booking-System.zip`, and
`References/VARIATION_INDEX.md` ([local tree](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/),
[local ZIP](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System.zip),
[local index](../../References/VARIATION_INDEX.md#order_processing_system));
[pinned tree](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System)
at `1698cc6`. The ZIP is an **exact duplicate archive**, not another variation.

#### Variation 4 — Order-state SLA monitor

- **Candidate prompt/scope delta:** Consume order-state-change events, retain entry
  time/history, and alert when a nonterminal order remains in a state beyond its
  configured SLA. The cloned code is an adjacent monitor, not an order solution.
- **Expected Senior solution:** `StateSlaPolicy`, append-only state history,
  idempotent event handler, due-state repository query, clock-injected monitoring
  job, and deduplicated alert publisher. Late/duplicate events cannot move time
  backward or emit the same breach repeatedly.
- **Staff extension/trade-offs:** Define event-time versus processing-time policy,
  partition/order events by order ID, outbox/inbox recovery, alert suppression and
  escalation, replay/backfill, and monitor-lag/error-budget metrics.
- **Canonical runnable solution / Python code fit:** Python supplies stable IDs,
  versions, guarded transitions, and an injected clock; add emitted transition facts
  and a separate monitor. The Java audit model shows the richer event/history shape.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/SwiggyInterview/`
and `References/VARIATION_INDEX.md` ([local implementation input](../../References/kumaransg-LLD/Low_level_Design_Problems/SwiggyInterview/),
[local index](../../References/VARIATION_INDEX.md#order_processing_system));
[pinned tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SwiggyInterview).
This is an **adjacent implementation input**, not a complete order-management answer.

#### Variation 5 — Priority-aware logistics order

- **Candidate prompt/scope delta:** Add order priority, payment details, origin and
  destination, and vehicle assignment/status. Ask where commercial order behavior
  ends and logistics scheduling begins.
- **Expected Senior solution:** Keep the commercial aggregate and payment reference
  stable; translate a confirmed order into a fulfilment request. Put priority and
  vehicle choice behind an allocation/scheduling policy, with guarded shipment
  transitions and no mutable shared status enum.
- **Staff extension/trade-offs:** Separate order and fulfilment bounded contexts,
  define fairness/aging so priority cannot starve normal work, handle reassignment
  and capacity races, and version the integration event contract.
- **Canonical runnable solution / Python code fit:** Reuse Python's commercial order
  snapshot and statuses, then explain a new fulfilment port. Priority, vehicles, and
  tasks belong in warehouse aggregates rather than the live-coded `Order`.

Provenance: `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LogisticsDesign/`
and `References/VARIATION_INDEX.md` ([local model input](../../References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LogisticsDesign/),
[local index](../../References/VARIATION_INDEX.md#order_processing_system));
[pinned tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/LogisticsDesign).
This compact model is an **adjacent variation**, not an end-to-end solution.

## Functional Requirements

1. Create an order from a customer, item quantities, price snapshots, currency, and
   delivery address.
2. Assign a unique order ID and retain an immutable snapshot of each ordered item.
3. Track order-level and line-level status, totals, discounts, and status history.
4. Confirm an order only after the required inventory and payment steps succeed.
5. Support full cancellation and eligible line-level cancellation.
6. Track partial and complete fulfilment without losing the original order details.
7. Expose order details and a chronological history of meaningful changes.
8. Make create, confirm, cancel, and fulfilment-update commands idempotent.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `Order` | Aggregate root that protects order invariants |
| `OrderLine` | Immutable item snapshot, quantity, price, and line status |
| `Money` | Amount and currency with exact arithmetic |
| `Address` | Delivery-address value object |
| `OrderStatus` | Commercial lifecycle state |
| `StatusChange` | Auditable transition with timestamp and reason |
| Integration ports | Inventory, payment, promotion, and fulfilment contracts |

## Business Rules and State Transitions

- Define and enforce valid transitions. A reasonable baseline is `PENDING` to
  `CONFIRMED`, then `PARTIALLY_FULFILLED` or `FULFILLED`; cancellation is allowed
  only while the affected quantity remains cancellable.
- Keep payment status and fulfilment status separate from `OrderStatus` to avoid a
  single state enum containing every possible combination.
- Quantity, unit price, currency, and applied-discount snapshots must not change
  after confirmation.
- An order cannot be fulfilled beyond its confirmed, non-cancelled quantity.
- Every rejected transition must return a domain-specific reason.

## Concurrency and Failure Handling

- Two concurrent commands must not confirm, cancel, or fulfil the same quantity
  twice. Use version checks or another explicit concurrency strategy.
- Retried requests with the same idempotency key must return the original result.
- If payment fails after inventory is reserved, release the reservation.
- If confirmation has an unknown outcome, reconciliation must be possible without
  creating a second charge or reservation.
- Record enough history to explain both successful and failed workflow steps.

## Demonstration Scenarios

1. Create, reserve, pay, confirm, and fulfil an order successfully.
2. Reject an order when one line cannot be reserved and release prior holds.
3. Handle payment failure without leaving inventory reserved.
4. Retry the same create or confirm command without duplicating work.
5. Cancel one line while the remaining lines continue to fulfilment.
6. Reject an invalid transition, such as cancelling an already fulfilled quantity.

## Existing Implementation Slice

The existing `Order.java` models an ID, priority, timestamp, description, and priority
levels. Treat it as the order-prioritisation slice of this problem, not as a complete
order management system. Extend or replace that model within this folder rather than
creating a second, duplicate order-system problem.

The Java reference solution below replaces that legacy priority-only slice with the
commercial `model/Order.java` aggregate; priority scheduling is not an order-domain
invariant and therefore is not carried into the aggregate.

## Extensions

- Returns and exchanges
- Split shipments and backorders
- Event-driven saga coordination
- Customer-visible delivery estimates

## Related Problems

- [Inventory Reservation Service](../inventory_reservation_service/README.md)
- [Coupon / Promotion Engine](../coupon_promotion_engine/README.md)
- [Payment Processing Service](../payment_processing_service/README.md)
- [Warehouse Fulfilment Domain](../warehouse_fulfilment_domain/README.md)

---

## Python 3 Reference Solution

### One-hour Python coding scope

**Implement live:** frozen `Money`, `OrderLine`, and `Order` snapshots; guarded
confirm/fulfil/cancel transitions; `Clock`, `InventoryPort`, and `PaymentPort`;
`OrderService.create_order`, `confirm_order`, and one line-update path; operation-
scoped receipt fingerprints; a per-order lock; stable downstream keys; and the
definitive-payment-failure versus unknown-payment compensation rule. Write the six
deterministic tests for exact money, guards, replay conflict, compensation, unknown
outcomes, and concurrent confirmation.

**Explain rather than type:** HTTP/status mapping, validation DTOs, ORM/repository
plumbing, durable idempotency storage, database locking, saga/outbox workers,
unknown-commit reconciliation, authentication, metrics, tracing, and retention.

- [One-hour implementation](python/solution.py)
- [Deterministic tests](python/test_solution.py)

Run with Python 3.9+ and only the standard library:

```bash
cd Problems/order_processing_system/python
python3 test_solution.py
```

## Java 17 Reference Solution

### Implementation Status

| Requirement | Status | Implementation |
| --- | --- | --- |
| Immutable order and price snapshots | Complete | `model/Order`, `model/OrderLine`, `model/Address` |
| Exact totals and discounts | Complete | `model/Money` uses `BigDecimal` plus `Currency`; no `double` |
| Explicit lifecycle and quantity invariants | Complete | Immutable aggregate transitions and domain exceptions |
| Inventory/payment/promotion/warehouse coordination | Complete | Interfaces in `port`; demo uses only in-memory adapters |
| Atomic concurrent commands | Complete | Fair command lock plus immutable returned snapshots |
| Create/confirm/cancel/fulfil idempotency | Complete | Operation-scoped key, input fingerprint, and original-result record |
| Failure compensation and audit history | Complete | Inventory release after payment failure; failed attempts enter history |
| Runnable scenarios | Complete | Success, payment failure, duplicate commands, line cancellation, and a two-thread race |

### Architecture and Design Choices

- `Order` is the aggregate root. It is immutable: confirmation, cancellation, and
  fulfilment return a new version only after validating the state and quantity
  invariants. Returned command results therefore remain safe snapshots.
- `OrderStatus`, `PaymentStatus`, and `FulfilmentStatus` are deliberately separate.
  This avoids an enum for every cross-product of commercial, money, and warehouse
  states. `StatusChange` keeps both successful transitions and workflow failures.
- `Money` accepts decimal strings, enforces the currency's scale with
  `RoundingMode.UNNECESSARY`, and checks currencies on arithmetic. Promotions return
  a per-unit discount so partial-cancellation refunds remain exact and deterministic.
- `OrderService` is the application/orchestration layer. A fair `ReentrantLock`
  makes a command atomic in this in-memory implementation. Its idempotency store is
  namespaced by operation, rejects key reuse with different input, and returns the
  original immutable result without invoking a dependency again.
- Inventory, payment, promotion, fulfilment, ID generation, and time are injected.
  The ports express idempotency keys but never make a real network call. The demo
  adapters prove integration behavior without hiding it behind a framework.
- Confirmation reserves first and charges second. A failed charge requests an
  idempotent release and records why the order remains `PENDING`. A production
  service would persist orders, command records, and an outbox in one transaction,
  then reconcile unknown remote outcomes using the stable downstream keys. The
  in-memory implementation similarly retries a failed fulfilment request on the
  next confirm retry, without charging or reserving again.
- The single-process lock is intentionally simple and correct for the exercise. In
  a multi-instance deployment, replace it with repository version checks or row
  locks while retaining the aggregate methods, unique idempotency constraint, and
  transactional outbox.

### Source Structure

```text
com/example/lld/order_processing_system/
├── model/
│   ├── Address.java
│   ├── Money.java
│   ├── Order.java
│   ├── OrderLine.java
│   ├── OrderLineRequest.java
│   ├── OrderStatus.java
│   ├── OrderLineStatus.java
│   ├── PaymentStatus.java
│   ├── FulfilmentStatus.java
│   └── StatusChange.java
├── service/
│   └── OrderService.java
├── port/
│   ├── IdGenerator.java
│   ├── InventoryPort.java
│   ├── PaymentPort.java
│   ├── PromotionPort.java
│   └── FulfilmentPort.java
├── exception/
│   ├── OrderDomainException.java
│   ├── OrderNotFoundException.java
│   ├── InvalidOrderStateException.java
│   ├── IdempotencyConflictException.java
│   └── ConfirmationException.java
└── demo/
    └── Main.java
```

### Compile and Run

Run from the repository root. The output directory is disposable and is created
under `/private/tmp` so no class files are written into the source tree.

```bash
ORDER_OUT="$(mktemp -d /private/tmp/order-processing.XXXXXX)"
find Problems/order_processing_system -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -d "$ORDER_OUT"
java -cp "$ORDER_OUT" com.example.lld.order_processing_system.demo.Main
```

Key output includes:

```text
Order Management System demo passed
  concurrent successes: 1/2
```
