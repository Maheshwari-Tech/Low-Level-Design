# Payment Processing Service

## Problem Description

Design a provider-agnostic payment service that safely handles authorisation,
capture, void, and refund operations. External provider calls may time out, fail, or
complete asynchronously, so retries and callbacks must not cause a duplicate charge.

Use opaque payment-method tokens. Collection or storage of raw card or banking
credentials is outside the scope of this problem.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design the low-level domain and service contracts for a provider-agnostic payment
> processor. A merchant creates an intent, authorises it, captures it fully or in
> parts, voids unused authorisation, and issues full or partial refunds. Provider
> requests can time out and provider callbacks can be late, duplicated, or out of
> order. Show how the design prevents duplicate money movement and remains
> extensible to another provider. Focus on boundaries, invariants, failure semantics,
> and one concrete flow; no UI or raw-card handling is required.

### Minute-by-minute plan

| Time | Candidate should drive | Interviewer signal |
| --- | --- | --- |
| 0-5 | Restate the money-moving use cases and name the irreversible external boundary. | Frames before drawing classes. |
| 5-12 | Ask scope questions; state assumptions and service ownership. | Separates order, vault, provider, and accounting concerns. |
| 12-20 | Model `Payment`, operations/attempts, money, and lifecycle invariants. | Uses behaviour-rich boundaries rather than mutable records. |
| 20-29 | Define client APIs, provider port, callback contract, and persistence identities. | Makes retry and correlation semantics explicit. |
| 29-41 | Resolve concurrency, idempotency, crash windows, timeouts, and stale callbacks. | Treats `UNKNOWN` as a first-class outcome. |
| 41-49 | Walk a partial-capture and refund scenario with a timeout and concurrent command. | Applies the model consistently under pressure. |
| 49-55 | Explain patterns, SOLID choices, rejected alternatives, and tests. | Justifies seams instead of pattern collecting. |
| 55-60 | Discuss production evolution, observability, and trade-offs; summarize guarantees. | Distinguishes a sound core from scale-driven additions. |

### Clarifying questions and strong assumptions

| Ask early | Strong working assumption if unanswered |
| --- | --- |
| Is this a gateway for one merchant or a multi-tenant platform? | Multi-merchant identities exist; idempotency and uniqueness are scoped by merchant. Merchant onboarding and pricing are out. |
| Is authorisation separate from capture? Are partial operations allowed? | Separate authorise/capture, multiple partial captures and refunds, and a void of unused authorisation are required. |
| Who chooses the provider, and may it change mid-payment? | A routing policy chooses before the first request. After an ambiguous request, failover is forbidden until reconciliation proves no money moved. |
| What are the provider response modes? | Synchronous accept/decline, transport failure, timeout, polling, and signed asynchronous callbacks all exist. |
| What does an idempotency key mean? | The caller supplies one per logical command; the same key and fingerprint replay the original result, while changed parameters conflict. |
| What money representation is expected? | Positive integer minor units plus ISO currency; one immutable currency per payment. Zero-decimal and non-two-decimal currencies use currency metadata. |
| Are credentials, fraud, settlement, or disputes included? | Only opaque method tokens cross this service. Risk is a pre-authorisation port; settlement, chargebacks, and ledger posting are downstream boundaries. |
| What consistency is promised to clients? | A command returns a durable operation status. Reads may lag provider reality while an attempt is `UNKNOWN`, but never fabricate failure or success. |

### Scoped requirements and exclusions

In scope:

- create an immutable payment intent and perform authorise, capture, void, and
  refund commands;
- one or more provider adapters with stable request correlation;
- partial-amount accounting, command idempotency, callback deduplication, audit
  history, and reconciliation of unknown outcomes;
- safe concurrent commands against the same payment and reliable domain-event
  publication.

Explicitly out of scope:

- PAN/bank credential collection, token vault internals, hosted checkout UI, fraud
  models, FX conversion, fees, accounting journals, settlement, disputes, payouts,
  subscriptions, and marketplace split payments;
- a claim of end-to-end exactly-once delivery or a distributed transaction with an
  external provider.

### Ownership boundaries

| Boundary | Owns | Does not own |
| --- | --- | --- |
| Order service | Commercial order, amount expected, cancellation intent | Provider attempts or proof of money movement |
| Payment service | Intent, operation identity, provider correlation, lifecycle, captured/refunded totals, operational audit | Raw credentials, general ledger, fulfilment decision |
| Token vault | Secure instrument/token lifecycle | Payment business state |
| Risk service | Risk decision and evidence | Provider settlement state |
| Provider adapter | Translation, authentication, provider-specific error/status mapping | Domain invariants or routing policy |
| Accounting/settlement | Immutable financial journal and settlement reconciliation | Retrying an operational provider command |

The payment aggregate is operational truth for what this service knows; the provider
is truth for what it executed, and the accounting ledger is truth for booked money.
Reconciliation connects those truths without pretending they are atomically updated.

### APIs and contracts

| Contract | Essential request fields | Result semantics |
| --- | --- | --- |
| `POST /payments` | `orderId`, `amountMinor`, `currency`, `methodToken`; `Idempotency-Key` | `paymentId`, summary status, version |
| `POST /payments/{id}/authorizations` | optional routing hints; key | Durable `operationId`; `SUCCEEDED`, `DECLINED`, `FAILED`, `PENDING`, or `UNKNOWN` |
| `POST /payments/{id}/captures` | `amountMinor`; key | Original result on replay; `409` for key/fingerprint conflict or amount race |
| `POST /payments/{id}/voids` | target authorisation/remaining amount; key | Voids only still-usable authorisation |
| `POST /payments/{id}/refunds` | `captureId` or payment scope, `amountMinor`, reason; key | Refund operation, not an in-place decrement without evidence |
| `GET /payments/{id}` | payment identity | Current projection, counters, unresolved operations, immutable history |
| `POST /provider-callbacks/{provider}` | raw signed body, provider event/reference IDs | Authenticate, normalize, deduplicate, correlate, and acknowledge independently of business disposition |

Every command key is stored with a canonical request fingerprint, processing state,
and frozen response. A replay with the same fingerprint returns the same
`operationId`; reuse for another amount, payment, or operation is a conflict.
Responses expose an operation status rather than a misleading Boolean. `202` is a
reasonable transport response while an operation remains `UNKNOWN` or pending.

The provider port should express domain-relevant capability, for example:

```text
submit(ProviderOperation{providerRequestId, type, amount, token, priorReference})
    -> SUCCEEDED | DECLINED | FAILED_DEFINITIVE | PENDING | UNKNOWN
query(providerRequestId) -> the same normalized outcome set
normalizeAndVerify(callback) -> ProviderEvent
```

`providerRequestId` is stable across transport retries. Provider references and raw
diagnostic codes may be retained securely, but adapters map them to a small common
taxonomy. Capability discovery must reject unsupported partial capture/refund rather
than simulate it.

### Invariants and lifecycle

- Intent amount and currency are immutable after processing starts; amounts use
  exact arithmetic and are positive.
- `captured + captureInFlight + voided + voidInFlight <= authorised` and
  `0 <= refunded + refundInFlight <= captured`. Reserving in-flight amounts closes
  the race before the provider call.
- A successful provider operation changes monetary totals exactly once. Declined,
  failed, and unknown attempts do not.
- Each logical operation has one stable provider request identity; a terminal attempt
  is immutable. A later contradictory event is quarantined for reconciliation.
- Capture requires a successful, unexpired authorisation. Refund requires settled
  captured value. A void cannot undo captured money; a refund cannot restore
  authorisation.
- The summary status is a projection of operations and counters, not the sole source
  of truth. This avoids a state explosion when some amount is captured, some is
  voided, and a refund is pending.

```text
CREATED -> AUTHORIZING -> AUTHORIZED -> PARTIALLY_CAPTURED -> CAPTURED
                 |             |               |
                 |             +-- void unused authorization
                 +-- DECLINED / FAILED / UNKNOWN (reconcile UNKNOWN)

successful captures -> PARTIALLY_REFUNDED -> REFUNDED
```

### Concurrency, idempotency, unknown outcomes, and compensation

| Concern | Strong design |
| --- | --- |
| Capture, void, or refund commands race | In one database transaction, compare aggregate version, validate the limit including pending reservations, create the operation, and reserve its amount. Retry only the losing local transaction. |
| Caller retries | Uniqueness on `(merchantId, operationType, idempotencyKey)` plus a payload fingerprint returns the frozen result; concurrent duplicates join the same durable operation. |
| Crash around provider call | Persist the pending operation and stable provider request ID before calling. A recovery worker submits/queries it. Success followed by a local crash is later recovered by query or callback. |
| Provider timeout | Mark `UNKNOWN`, keep its amount reserved, and query or await a callback. Never interpret timeout as decline and never issue a new provider identity blindly. |
| Duplicate/out-of-order callback | Verify first, deduplicate provider event ID, then apply a conditional monotonic transition to the correlated attempt. Arrival time alone never determines truth. |
| Retryable event publication | Commit payment change and outbox record together; publish at least once. Consumers deduplicate by event ID and aggregate version. |
| Business rollback | Money movement is not rolled back locally. Void an authorisation or refund a capture as a new, auditable compensating operation; retry/query that compensation with its own stable identity. |

There is no honest exactly-once guarantee across the database and provider. The goal
is effectively-once business effect through durable identities, atomic local
invariants, provider idempotency where available, and reconciliation where it is not.

### Concrete scenario walkthrough

1. Order `O-42` creates payment `P-42` for USD 100.00. Repeating key `create-42`
   returns `P-42`; using that key for USD 120.00 conflicts.
2. Authorisation operation `A-1` is persisted with provider request `pr-A-1`, then
   succeeds for USD 100.00. The callback arriving after the synchronous response is
   a harmless duplicate transition.
3. Captures `C-70` and `C-40` race for USD 70.00 and USD 40.00. The first transaction
   reserves 70.00; the second sees only 30.00 available and is rejected before any
   provider call.
4. `C-70` times out. It stays `UNKNOWN` and its 70.00 remains in flight. Retrying key
   `capture-70` returns `C-70` and uses `pr-C-70`; it does not create another charge.
5. A signed callback confirms `pr-C-70`. One conditional transition records 70.00
   captured. A new USD 30.00 capture can now complete, bringing captured total to
   100.00.
6. Refund `R-20` for USD 20.00 succeeds. Its duplicate callback changes neither the
   USD 20.00 refunded total nor history. If the order had been cancelled while the
   authorisation was unknown, reconciliation would first settle `A-1`, then issue a
   distinct void if it had succeeded.

### Expected solution and design-principle reasoning

A strong solution uses a `Payment` aggregate for local amount/lifecycle invariants,
immutable `PaymentOperation`/attempt records for external effects, and an application
service for orchestration. Repositories, an idempotency store, outbox, provider port,
clock, and ID source sit behind explicit interfaces.

- **SRP:** the aggregate protects money rules; the application service sequences
  work; adapters translate providers; reconciliation repairs knowledge. None should
  absorb all four responsibilities.
- **OCP and DIP:** routing and provider integrations depend on a domain-owned port, so
  a new provider or routing policy is added without changing monetary rules.
- **ISP:** capability-focused contracts avoid forcing a provider that cannot void or
  partially capture to fake support.
- **LSP:** every adapter must preserve normalized semantics, stable correlation, and
  idempotency expectations; merely sharing a method signature is insufficient.
- **Adapter** is justified by incompatible provider protocols. **Strategy** is useful
  for replaceable routing policy. A guarded transition table/domain methods are
  clearer than State classes until state-specific behaviour becomes large. Repository
  plus transactional outbox handles persistence/publication; a saga supplies forward
  compensation across service boundaries.
- Reject a generic `processPayment(): boolean`, floating-point money, blind retry or
  provider failover, a global lock, and distributed two-phase commit. Event sourcing
  is optional only when audit/replay needs justify its operational cost; append-only
  attempts plus a current projection are a simpler starting point.

The design stays testable with scripted fake providers, deterministic clocks/IDs,
adapter contract tests, invariant/property tests over operation sequences, barrier-
based concurrency tests, and fault injection before/after the provider call and
local commit. No singleton or static provider client should hide dependencies.

### Senior and Staff expectations

| Decision | Strong Senior baseline | Staff-level evolution trigger and response |
| --- | --- | --- |
| Consistency | One payment consistency boundary with versioned writes and pending-amount reservations | Partition by merchant/payment; keep per-payment serialization while building asynchronous read models |
| Provider routing | Capability-aware adapter plus simple configured policy | Add cost, geography, health, and approval-rate policy with explainability; never fail over an ambiguous operation |
| Persistence | Relational current state, immutable attempts, idempotency rows, outbox | Append-only financial operation ledger or event sourcing only for demonstrated audit/replay needs |
| Reconciliation | Callback plus targeted polling for `UNKNOWN` | Provider settlement-file ingestion, discrepancy queues, aging SLOs, and operator repair tooling |
| Service boundaries | Operational payments emits facts to accounting/risk/order | Versioned event contracts, data-retention policy, regional/tenant isolation, and explicit ownership of disputes and payouts |
| Reliability | Metrics by provider/outcome and alert on stuck unknowns | Error budgets, provider circuit policy, dark launches, chaos tests, and blast-radius controls per merchant/provider |

Staff signal is not more classes. It is preserving the core invariants while making
cross-team contracts, reconciliation, operability, and migration paths explicit.

### Interviewer follow-ups with concise strong answers

| Question | Strong answer |
| --- | --- |
| Why is a timeout not failure? | The provider may have committed after our read deadline. Failure would release funds for another attempt and permit a duplicate charge; keep it unknown and reconcile. |
| Can this be exactly once? | Not atomically across two systems. Stable command/provider IDs, deduplication, local transactions, and reconciliation provide effectively-once business effect. |
| Same key, different amount? | Reject with an idempotency conflict; silently returning or overwriting a result hides a caller bug and can move the wrong amount. |
| What if the callback beats the API response? | Both target the same attempt with a conditional transition. Whichever commits first establishes the terminal result; the other observes an idempotent terminal state. |
| What if success occurs, then the service crashes before saving it? | The pre-persisted provider request ID lets polling or a callback recover success. Do not create a fresh request ID. |
| How do partial captures remain safe concurrently? | Atomically reserve pending amount against authorised balance before I/O; terminal success converts reserved to captured and terminal failure releases it. |
| When is provider failover safe? | Before dispatch, or after authoritative evidence says the first provider did not execute. It is unsafe merely because the first call timed out. |
| A provider sends success after a local cancellation—what then? | Correlate and record the real success, then issue a new void/refund compensation as allowed. Never rewrite history to match desired business state. |
| How are webhooks secured? | Verify the signature over the raw body, timestamp/replay window, endpoint/provider identity, and rotated secret before deduplication or mutation. |
| How long are idempotency records kept? | At least the documented client/provider retry horizon and operational dispute window; archive compact fingerprints/results if full payload retention is restricted. |
| How do you model multi-currency? | One payment has one currency and exact minor units. FX uses an immutable quote and separate accounting entries, not arithmetic inside capture/refund. |
| Why not one State class per status? | Most rules depend on amounts and attempts as well as status. Guarded domain operations are clearer; introduce State objects only when behaviour truly varies enough. |
| What should an upstream event contain? | Stable event/payment/operation IDs, type, amount/currency, normalized outcome, aggregate version, and occurred time—no raw credentials. |
| What is the highest-value test? | A fault/concurrency matrix proving no sequence of retries, timeouts, callbacks, and racing partial operations violates captured/refunded limits or applies an effect twice. |

### Red flags

- Models provider result as only success/failure, or retries/fails over after timeout
  with a new request identity.
- Uses `double`/`float`, mutable currency, or checks amount limits outside the atomic
  write that consumes them.
- Treats an HTTP idempotency header as sufficient without a durable record,
  fingerprint, and response semantics.
- Lets callbacks set payment status directly by arrival order, with no signature,
  correlation, deduplication, or terminal-transition guard.
- Couples provider SDK types to the aggregate, uses a singleton/global lock, or
  proposes distributed two-phase commit with the provider.
- Says “refund rolls back capture” instead of modelling a new external money-moving
  operation and its possible unknown outcome.
- Adds factories, strategies, State objects, and event sourcing without naming the
  variation or requirement each one serves.

### Interview variations and expected solutions

These variations synthesize the distinct source prompts and implementation ideas at
`low-level-design-primer@49fe9f2`, `awesome-low-level-design@fc26e40`, and
`kumaransg/LLD@1698cc6`. Unlicensed material is summarized; teaching fragments are
inputs to critique, not reference solutions.

#### Variation 1 — Core payment system

- **Candidate prompt / scope delta:** Expand the primer's terse “Design Payment
  System” prompt into the canonical authorise, capture, void, refund, retry, and
  callback scope. No additional product surface is implied.
- **Expected Senior solution:** Model exact `Money`, a payment aggregate, immutable
  operations/attempts, legal transitions, pending-amount reservations, durable
  idempotency, a provider port, and reconciliation of `UNKNOWN` outcomes.
- **Staff-level extension / trade-offs:** Define operational-payment versus ledger
  truth, settlement-file reconciliation, unknown-aging SLOs, tenant isolation, data
  retention, outbox event compatibility, and an incremental scale path without
  weakening per-payment serialization.
- **Canonical runnable solution:** Applies directly and is the expected executable
  baseline. A production answer replaces in-memory maps/locks with durable unique
  constraints, versioned transactions, an outbox, and recovery workers.

Provenance: [`References/PRIMER_QUESTION_INDEX.md`](../../References/PRIMER_QUESTION_INDEX.md)
maps [`References/low-level-design-primer/questions.md`](../../References/low-level-design-primer/questions.md)
to this problem; see the pinned [L77 prompt](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L77).

#### Variation 2 — Merchant-facing gateway like Razorpay

- **Candidate prompt / scope delta:** Expose payments as a multi-merchant public
  gateway with API credentials, merchant-scoped idempotency, signed webhooks, and
  routing across upstream processors. Merchant onboarding, pricing, and payouts
  should be explicitly accepted or excluded.
- **Expected Senior solution:** Keep the same payment/operation core, but scope every
  identity and uniqueness rule by merchant; authenticate commands; verify and replay-
  protect webhooks; expose stable operation resources; model provider capabilities;
  and never fail over an ambiguous request.
- **Staff-level extension / trade-offs:** Add key rotation, per-tenant quotas and
  blast radius, regional data/compliance boundaries, capability/cost/SLO-aware routing,
  adapter rollout controls, merchant reconciliation exports, and versioned public
  contracts. Strong routing optimizes approval rate without sacrificing ambiguity
  safety.
- **Canonical runnable solution:** Payment invariants, attempt correlation,
  idempotency, callbacks, and unknown handling carry over. Add merchant/auth models,
  persistent distributed stores, webhook security, routing telemetry, and operational
  reconciliation; do not put raw credentials into the aggregate.

Provenance: the distinct gateway prompt is in
[`References/low-level-design-primer/questions.md`](../../References/low-level-design-primer/questions.md)
and the local [`PRIMER_QUESTION_INDEX`](../../References/PRIMER_QUESTION_INDEX.md);
see pinned [L79](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L79).

#### Variation 3 — Legacy or third-party provider adapter

- **Candidate prompt / scope delta:** Integrate an incompatible legacy gateway while
  keeping checkout and domain code provider-neutral. The source is an Adapter-pattern
  exercise, not a complete money-moving service.
- **Expected Senior solution:** Define a domain-owned, capability-aware provider port;
  map vendor requests, references, error codes, pending states, and callbacks in an
  adapter; preserve stable request identity; and test normalized semantics. Replace
  Boolean success and floating amounts with typed outcomes and exact `Money`.
- **Staff-level extension / trade-offs:** Add adapter contract certification,
  version/capability negotiation, shadow/canary traffic, secret rotation, provider-
  specific rate/circuit controls, and safe rollback. Fallback is allowed before
  dispatch, not merely after a timeout.
- **Canonical runnable solution:** Its `PaymentProvider` port and provider request IDs
  apply directly. Only a concrete adapter and capability metadata are new; vendor SDK
  types must remain outside `Payment`.

Provenance: the Java
[`References/awesome-low-level-design/design-patterns/java/adapter/`](../../References/awesome-low-level-design/design-patterns/java/adapter/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/java/adapter))
and thin car-rental
[`PaymentProcessor`](../../References/awesome-low-level-design/solutions/java/src/carrentalsystem/payment/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/carrentalsystem/payment))
are pedagogical **fragments**. Their C++, C#, Go, and Python ports are collapsed as
language **duplicates**, not separate domain variants.

#### Variation 4 — Payment method or provider strategy

- **Candidate prompt / scope delta:** Support credit card, UPI, PayPal, cash-like, or
  other tender choices behind a common checkout contract. Clarify whether the
  variation is instrument behaviour, provider routing, or both.
- **Expected Senior solution:** Store only an opaque method token; represent method
  capabilities and asynchronous requirements explicitly; use Strategy for a real
  selection/routing policy and Adapter for protocol translation. Do not force every
  method through `pay(double): boolean` or fake unsupported capture/refund behaviour.
- **Staff-level extension / trade-offs:** Model redirect/step-up sessions and
  `REQUIRES_ACTION`, policy explainability, risk/cost/regional inputs, method-token
  lifecycle, and contract conformance. Prefer composition of capabilities over a
  fragile subclass tree.
- **Canonical runnable solution:** `PaymentMethodToken`, provider routing, attempts,
  and amount invariants remain. Extend the normalized outcome/capability model for
  user action and method-specific expiry; the core must still reconcile unknowns.

Provenance: the
[`References/awesome-low-level-design/.../onlineshoppingservice/strategy/`](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/strategy/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/onlineshoppingservice/strategy))
and movie/car-rental language ports are equivalent teaching **fragments**. Kumar's
[`SystemDesign/VendingMachine`](../../References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/VendingMachine/)
([pinned](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/VendingMachine))
adds a cash/change-oriented tender fragment; its `Leetcode/G4G` copy is an **exact
duplicate** recorded by [`References/VARIATION_INDEX.md`](../../References/VARIATION_INDEX.md#exact-duplicate-paths).

#### Variation 5 — Payment inside an order workflow

- **Candidate prompt / scope delta:** Validate payment against an order bill and move
  the order forward or cancel it. The supplied food-delivery and hotel examples are
  embedded checkout fragments, not standalone payment processors.
- **Expected Senior solution:** Keep Order as owner of commercial status and Payment
  as owner of provider truth. Pass an immutable amount/order reference, emit a
  payment fact through an outbox, and let an order saga react. A decline may change
  the order; a timeout must not be reported as a decline.
- **Staff-level extension / trade-offs:** Define orchestration versus choreography,
  event versions and dedupe, cancellation races, expiry policy, compensation owners,
  and repair tooling for orders whose payment remains ambiguous.
- **Canonical runnable solution:** The complete payment core applies; add an order
  boundary and events rather than allowing `PaymentService` to mutate Order. The
  fragment's direct coupling, process-local maps, and simplistic status changes need
  adaptation.

Provenance: Kumar's
[`References/kumaransg-LLD/.../fooddelivery/services/PaymentService.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/src/main/java/com/mayank/fooddelivery/services/PaymentService.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/src/main/java/com/mayank/fooddelivery/services/PaymentService.java))
and [`HotelManagmentSystem/PaymentService.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/HotelManagmentSystem/PaymentService.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/HotelManagmentSystem/PaymentService.java))
are summarized as **fragment inputs**, not expected solutions.

#### Variation 6 — Stored methods, gateways, and receipts

- **Candidate prompt / scope delta:** Add a customer wallet of payment methods, a
  chosen gateway, and an immutable receipt linked to a ride/booking. This is a
  payment-profile and projection problem around the processing core.
- **Expected Senior solution:** Put raw card data in a compliant vault and store only
  tokens plus safe display metadata; use exact `Money`; make receipt identity derive
  from a successful operation; and separate customer method lifecycle from provider
  attempt lifecycle.
- **Staff-level extension / trade-offs:** Define PCI scope, consent and deletion,
  token portability, network-token updates, gateway capability/routing, audit access,
  and how immutable receipts coexist with later refunds or disputes.
- **Canonical runnable solution:** `PaymentMethodToken`, provider selection,
  operations, and refund history apply. Add a wallet/profile boundary and receipt
  projection; do not copy the fragment's mutable `Double` amounts or raw-card field.

Provenance: the ride-sharing
[`PaymentMethod`, `PaymentGateway`, and `PaymentReceipt` entities](<../../References/kumaransg-LLD/Ride Sharing /uber/src/main/java/lowleveldesign/uber/api/core/entities/>)
come from the pinned
[entity tree](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Ride%20Sharing%20/uber/src/main/java/lowleveldesign/uber/api/core/entities).
The adjacent empty payment client/proxy are **placeholders**, so this material is a
model **fragment**, not a solution. The separately found
[`ledger_company_navi/PaymentProcessor.java`](../../References/kumaransg-LLD/ledger_company_navi/src/main/java/com/navi/ledger/payment/PaymentProcessor.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/ledger_company_navi/src/main/java/com/navi/ledger/payment/PaymentProcessor.java))
is a loan-repayment schedule name collision and is deliberately not treated as a
gateway variation.

### Scoring rubric

Score each dimension from 0 to 4: `0` absent/unsafe, `2` workable with important
gaps, `4` precise and pressure-tested.

| Dimension | 4-point evidence |
| --- | --- |
| Domain model and invariants | Exact money; attempt/operation identity; legal lifecycle; pending amounts included in capture/refund limits |
| Contracts and ownership | Clear service/provider/accounting boundaries; usable APIs; normalized capability-aware provider port |
| Reliability and concurrency | Durable idempotency, atomic reservations, crash-window reasoning, unknown reconciliation, safe callbacks/outbox |
| Design reasoning and evolution | SOLID/pattern choices tied to variation; credible rejected alternatives; scale and migration trade-offs |
| Communication and testing | Drives assumptions and scenario; names guarantees honestly; proposes deterministic, concurrency, contract, and fault tests |

Interpretation: `17-20` strong Senior with Staff evidence, `13-16` Senior-ready,
`9-12` mixed, and `0-8` below bar. A Staff recommendation additionally requires a
`4` in reliability and design evolution plus cross-team/operability reasoning.
Blind retry after an unknown outcome or a design that can exceed monetary limits is
a safety veto regardless of total score.

## Functional Requirements

1. Create a payment intent for an order, exact amount, and currency.
2. Authorise a payment through a selected provider.
3. Capture an authorised amount fully or partially.
4. Void an uncaptured authorisation.
5. Refund a captured amount fully or partially.
6. Route operations through interchangeable payment-provider adapters.
7. Reconcile synchronous responses with duplicate or out-of-order callbacks.
8. Make every externally retried command idempotent.
9. Expose current state and an immutable attempt/transition history.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `Payment` | Aggregate containing amount, state, and captured/refunded totals |
| `PaymentAttempt` | One provider interaction and its outcome |
| `PaymentMethodToken` | Opaque reference to a stored payment method |
| `Money` | Exact amount and currency value object |
| `Refund` | Full or partial reversal and status |
| `PaymentProvider` | Provider-specific adapter contract |
| `ProviderCallback` | Normalised asynchronous result |
| `IdempotencyRecord` | Original result for a retried command |

## Business Rules and State Transitions

- Define and enforce valid states such as `CREATED`, `AUTHORIZING`, `AUTHORIZED`,
  `PARTIALLY_CAPTURED`, `CAPTURED`, `VOIDED`, `FAILED`, and `REFUNDED`.
- Captured total cannot exceed authorised total.
- Refunded total cannot exceed captured total.
- Amount and currency are immutable after processing begins.
- Declines, validation errors, transient failures, and unknown outcomes are distinct.
- Retry only operations that can be made safe through idempotency or reconciliation.

## Concurrency and Failure Handling

- Concurrent capture or refund commands must not exceed the permitted amount.
- Repeating a command with the same idempotency key returns its original result.
- A timeout is an unknown outcome, not proof that the provider failed.
- Duplicate callbacks must be harmless; stale callbacks must not regress state.
- Persist the attempt before or with enough correlation data to reconcile a timeout.

## Demonstration Scenarios

1. Create, authorise, and capture a payment successfully.
2. Handle a provider decline without retrying it as a transient failure.
3. Reconcile a timeout followed by a successful callback.
4. Retry a capture without capturing twice.
5. Perform two valid partial captures and reject an excessive capture.
6. Perform a partial refund and ignore its duplicate callback.

## Extensions

- Provider routing and failover policy
- Fraud and risk checks
- Multi-currency settlement
- Reconciliation reports and disputes

## Related Problems

- [Order Management System](../order_processing_system/README.md)
- [Vending Machine](../vending_machine/README.md)
- [Online Bookstore](../online_bookstore/README.md)

## Python 3 Reference Solution

- [`python/solution.py`](python/solution.py) is a standard-library implementation with
  exact `Decimal` money, immutable dataclasses, a provider `Protocol`, thread-safe
  aggregate transitions, concurrent idempotency joining, stable provider request IDs,
  partial capture/refund limits, and late/duplicate callback reconciliation.
- [`python/test_solution.py`](python/test_solution.py) deterministically covers exact
  totals, concurrent duplicate commands, racing excessive captures, idempotency
  conflicts, and an `UNKNOWN` authorisation recovered by a late callback.

### One-hour Python coding scope

Implement live: `Money`, the operation/outcome enums, immutable attempt/snapshot
records, `PaymentProvider`, and `PaymentService.create/authorize/capture/refund`.
Use one `RLock` per payment, a small idempotency entry with an `Event`, and tests for
same-key concurrent retry, partial-amount limits, and timeout-to-callback
reconciliation. Add void or stale-callback handling if time remains.

Explain rather than type: HTTP/webhook authentication, ORM mappings and database
unique/version constraints, persistent command records, transactional outbox,
recovery workers, distributed storage, PCI deployment boundaries, settlement-file
reconciliation, metrics, alerts, and dashboards.

Run with Python 3.9+ and only the standard library:

```bash
cd Problems/payment_processing_service/python
python3 test_solution.py
```

## Java 17 Reference Solution

**Status:** Complete, runnable Java 17 reference implementation. It covers creation,
authorisation, partial capture, void, partial refund, exact money arithmetic,
provider routing, timeouts, late/duplicate/out-of-order callbacks, command
idempotency, immutable history snapshots, and same-payment concurrency safety.

### Architecture

| Layer | Important types | Design role |
| --- | --- | --- |
| Domain | `Payment`, `Money`, `PaymentAttempt`, `PaymentSnapshot` | Owns lifecycle and amount invariants; exposes immutable snapshots |
| Application | `PaymentService` | Coordinates idempotency, provider calls, callback routing, and aggregate lookup |
| Provider port | `PaymentProvider`, `ProviderRequest`, `ProviderResponse` | Keeps provider-specific integration outside the domain |
| Test adapter | `FakePaymentProvider` | FIFO deterministic outcomes and reproducible terminal callbacks |
| Errors | `PaymentDomainException` subclasses | Distinguishes missing data, illegal states, amount limits, conflicts, and callback errors |

The aggregate permits only one unresolved provider request at a time. This matters
after a timeout: the result is `UNKNOWN`, the request remains pending, and another
capture/refund cannot accidentally exceed a total whose provider outcome is still
uncertain. A terminal callback settles that exact provider request.

`PaymentService` uses a `ConcurrentHashMap` future per idempotency key. Concurrent
retries with the same key wait for and receive the original frozen `CommandResult`;
reusing the key for different parameters raises `IdempotencyConflictException`.
Provider request IDs are also stable at the provider boundary.

Each `Payment` mutation is synchronized independently, so different payments do not
share a domain lock. Capture and refund limits are checked and mutated in that same
critical section. Attempt records, audit entries, command results, and returned lists
are immutable. Duplicate callback IDs return `DUPLICATE`; a different callback for
an already-settled attempt returns `STALE`, so neither can regress state.

### Implemented Lifecycle

```text
CREATED -> AUTHORIZING -> AUTHORIZED -> PARTIALLY_CAPTURED -> CAPTURED
                 |              |                  |              |
                 |              +-----> VOIDED     +---- refund --+
                 |                                             |
                 +-----> DECLINED / FAILED          PARTIALLY_REFUNDED -> REFUNDED
```

- Authorisation must equal the immutable intent amount.
- Capture total never exceeds the authorised amount.
- Refund total never exceeds the captured total.
- A decline is terminal for authorisation; a transient failure is distinct and may
  be retried with a new idempotency key.
- An unknown outcome is not treated as failure and must be reconciled.

### Source Tree

```text
src/main/java/com/example/lld/payment_processing_service/
├── Demo.java
├── domain/
│   ├── AttemptStatus.java
│   ├── AuditEntry.java
│   ├── CallbackDisposition.java
│   ├── CallbackResult.java
│   ├── CommandResult.java
│   ├── Money.java
│   ├── Payment.java
│   ├── PaymentAttempt.java
│   ├── PaymentMethodToken.java
│   ├── PaymentOperation.java
│   ├── PaymentSnapshot.java
│   └── PaymentStatus.java
├── exception/
│   ├── AmountLimitExceededException.java
│   ├── DuplicatePaymentException.java
│   ├── IdempotencyConflictException.java
│   ├── InvalidMoneyException.java
│   ├── InvalidPaymentStateException.java
│   ├── OperationInProgressException.java
│   ├── PaymentDomainException.java
│   ├── PaymentNotFoundException.java
│   └── ProviderCallbackException.java
├── provider/
│   ├── FakePaymentProvider.java
│   ├── PaymentProvider.java
│   ├── ProviderCallback.java
│   ├── ProviderOutcome.java
│   ├── ProviderRequest.java
│   └── ProviderResponse.java
└── service/
    └── PaymentService.java
```

### Compile and Run

From this problem directory:

```bash
mkdir -p out
javac --release 17 -Xlint:all -d out $(find src/main/java -name '*.java' | sort)
java -cp out com.example.lld.payment_processing_service.Demo
```

The demo fails fast with `AssertionError` and covers successful partial captures,
an excessive capture, provider decline, an authorisation timeout followed by a late
success, a stale callback, an idempotent capture replay, a partial refund followed
by a duplicate callback, and voiding an uncaptured authorisation.
