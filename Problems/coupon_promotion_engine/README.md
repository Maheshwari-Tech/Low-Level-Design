# Coupon / Promotion Engine

## Problem Description

Design an extensible engine that evaluates automatic promotions and coupon codes
against a cart. It must return a deterministic price adjustment breakdown and enforce
redemption limits without hard-coding every campaign as a new conditional branch.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design the core objects and contracts for a coupon and automatic-promotion engine
> used by checkout. A cart may qualify by product, category, subtotal, customer,
> merchant, channel, and time. Promotions may be percentage, fixed amount, BOGO, or
> free shipping; some stack and some conflict. The caller needs an explained preview,
> then an idempotent redemption when an order is placed. Global and per-customer limits
> must remain correct when checkouts race or retry. Focus on boundaries, deterministic
> pricing, extensibility, and failure semantics rather than an admin UI.

The candidate should drive ambiguity down before drawing classes. Do not volunteer a
rule DSL, exhaustive “best basket” optimization, or a distributed architecture unless
their requirements make one necessary.

### Minute-by-minute plan

| Time | Candidate activity | Interviewer signal |
| --- | --- | --- |
| 0–5 | Clarify preview versus redemption, stacking policy, money, scale, and ownership | Finds the consistency boundary before naming patterns |
| 5–10 | State scope, exclusions, invariants, and one concrete policy | Converts ambiguity into testable decisions |
| 10–20 | Model campaign versions, conditions, benefits, quote lines, and redemption ledger | Keeps pricing definitions separate from mutable usage |
| 20–30 | Define APIs and walk the deterministic candidate/filter/select/apply pipeline | Explains ordering, conflicts, allocation, and rejection reasons |
| 30–42 | Design the atomic redemption transaction, idempotency record, reversal, and race handling | Handles the final-use race and lost response precisely |
| 42–49 | Trace the scenario below with exact amounts and state changes | Makes every invariant observable in output or storage |
| 49–55 | Discuss testing, SOLID/pattern choices, and rejected alternatives | Uses patterns to solve change, not as decoration |
| 55–60 | Evolve toward hot campaigns, multi-region reads, reservations, and operations | Separates a sound Senior core from Staff-level evolution |

### Clarifying questions and strong assumptions

| Ask this | Strong assumption if the interviewer leaves it open |
| --- | --- |
| Is a price preview authoritative? When is quota consumed? | `preview` is side-effect free. `redeem` revalidates the cart fingerprint, campaign versions, time, and quota in one authoritative transaction. |
| Are coupon codes and automatic campaigns selected differently? | The engine shortlists all active automatic campaigns plus only normalized codes supplied by the customer; unknown codes produce an explained rejection. |
| What does “stackable” mean? | Campaigns have priority, an optional exclusive flag, and an optional stacking group. Default winner in a group is highest priority, then campaign ID; compatible groups apply in stable order. |
| Must the engine find the mathematically largest discount combination? | No. It implements the declared deterministic policy. Arbitrary optimal subset search is excluded because dependencies can make it exponential and hard to explain. |
| Which limits exist and can redemption be undone? | Global and per-customer limits are enforced at redemption. Cancellation may append one idempotent reversal if that immutable campaign version allows it. |
| Can a campaign change while an order is in flight? | Published versions are immutable. A quote records version IDs and an expiry; edits create a new version and stale quotes must reprice. |
| What are the money and tax rules? | One currency per cart, integer minor units, explicit half-up percentage rounding, and stable residual allocation by line ID. Tax calculation is owned downstream and consumes the discount breakdown. |
| How large is the candidate set? | The catalog indexes by tenant, time, code, channel, and coarse product facts; the pure evaluator receives a bounded shortlist. Cache hits never authorize redemption. |
| Are inventory, shipping rates, customer segments, and merchant data owned here? | No. They arrive as versioned facts. The engine owns discount decisions, not the truth or availability of those facts. |
| Is checkout a long-running reservation flow? | Not initially: quota is consumed near order placement. A lease-based reservation is a later option only if the business accepts expiry and compensation semantics. |

### Scoped requirements, exclusions, and ownership boundaries

**In scope:** immutable campaign publication; automatic and entered-code discovery;
composable eligibility; percentage/fixed/BOGO/free-shipping benefits; deterministic
conflict resolution; itemized applied and rejected results; atomic redemption,
idempotent replay, query, and policy-controlled reversal.

**Explicitly out of scope:** campaign-authoring UI and approval workflow; a general
purpose rule language; tax, inventory, payment, and order state machines; cross-currency
conversion; fraud adjudication; recommendation/experimentation; globally optimal
discount search; long-lived coupon reservation.

| Boundary | Owns | Contract with this engine |
| --- | --- | --- |
| Campaign catalog | Definitions, immutable versions, publication lifecycle, coarse indexes | Returns a bounded versioned candidate set; cache is advisory |
| Pricing evaluator | Eligibility, conflict policy, benefit calculation, allocation, explanations | Pure function of facts + campaign versions + evaluation instant |
| Redemption ledger | Idempotency records, usage counters, committed/reversed redemptions | The sole write authority for limits; exposes atomic commit/query/reverse |
| Cart/order service | Cart snapshot, order identity, order/cancellation lifecycle | Supplies a stable fingerprint and retries the same operation key after uncertainty |
| Catalog/segment/shipping services | SKU/category/merchant/customer/channel/shipping facts | Supply versioned facts; are not called from inside the redemption transaction |
| Tax/payment services | Tax and collection after pricing | Consume allocated discount lines; never mutate promotion usage directly |

### APIs and contracts

```text
preview(PriceRequest) -> PriceQuote
  PriceRequest  { tenantId, cartSnapshot, customerFacts, channel,
                  enteredCodes[], evaluationTime? }
  PriceQuote    { quoteId, expiresAt, cartFingerprint, campaignVersionIds[],
                  subtotal, shipping, applied[], rejected[], totalDiscount, payable }

redeem(RedeemRequest, Idempotency-Key) -> RedemptionResult
  RedeemRequest { orderId, quoteId, cartFingerprint }
  Result        { redemptionId, status, applied[], payable, replayed }

reverse(RedemptionId, reason, Idempotency-Key) -> ReversalResult
getRedemption(orderId | redemptionId) -> RedemptionResult
publishCampaign(draft, expectedRevision) -> PublishedCampaignVersion
```

- `preview` captures one evaluation instant and returns campaign version IDs, stable
  adjustment IDs, allocation by cart line, and machine-readable plus human-readable
  rejection reasons. Identical inputs and versions produce identical output.
- `redeem` returns the stored result on an equal-key/equal-request replay; the same key
  with a different request hash is `409 IDEMPOTENCY_CONFLICT`.
- A stale cart/version/quote is `409 REPRICE_REQUIRED`; unavailable quota is
  `409 LIMIT_EXHAUSTED`. Neither is a transport retry.
- Redemption is all-or-nothing for the quoted promotion set. If any required finite
  quota cannot be consumed, no counter changes and checkout obtains a fresh preview.
- Internal benefit contracts return proposed adjustments; they do not mutate the cart
  or usage counters. The coordinator validates and applies adjustments to a pricing
  state in the declared order.

### Invariants and lifecycle

```text
DRAFT -> SCHEDULED -> ACTIVE -> ENDED -> ARCHIVED
                    \-> PAUSED -> ACTIVE
```

- A published campaign version is immutable; lifecycle changes are guarded transitions,
  and validity windows are half-open: `[startsAt, endsAt)`.
- `0 <= totalDiscount <= discountableAmount`; no line becomes negative, currencies
  match, and allocated line discounts sum exactly to their parent discount.
- Evaluation order is total and stable—priority descending, campaign ID ascending—so
  map iteration, thread timing, or repository order cannot change a quote.
- At most one winner applies per stacking group. An exclusive winner prevents every
  lower-ranked candidate; every skipped candidate receives a stable reason code.
- A quote consumes no quota and promises no future availability. A committed
  redemption references the exact campaign versions and monetary breakdown used.
- For each finite campaign, recorded active redemptions equal its usage count. One
  `(orderId, campaignVersionId)` can contribute at most one active use.
- `COMMITTED -> REVERSED` is monotonic. Reversal appends evidence and restores each
  eligible counter exactly once; it never deletes the original redemption.

### Concurrency, idempotency, retries, and unknown outcomes

The authoritative `redeem` path uses one database transaction:

1. Insert or read `(tenantId, operation, idempotencyKey, requestHash)`. Return its stored
   result on replay and reject a hash mismatch.
2. Lock/conditionally update finite-usage rows in ascending campaign-version ID order,
   revalidate immutable versions, quote expiry, cart fingerprint, and time, and rerun
   deterministic pricing if policy requires it.
3. Verify each counter with `used < limit`, write one redemption plus its lines, and
   increment all applicable global/customer counters atomically. A unique order/version
   constraint is a second defense against double consumption.
4. Store the serialized response with the idempotency record and commit. Publish an
   audit event through an outbox after commit; events never authorize the discount.

Two checkouts racing for the final use serialize on the quota row or use an atomic
conditional update; exactly one commits. Locking all campaign rows in a stable order
avoids cross-campaign deadlocks.

A database serialization error may be retried with bounded jitter because no transaction
committed. Validation failures are not retried. If the client times out after commit, it
must retry the **same** key or query by order ID; a new key is a new operation. An order
failure after redemption is a saga boundary: the order service sends a distinct,
idempotent reversal command. “Maybe committed” is never guessed from an HTTP timeout.

### Concrete scenario walkthrough

Cart `C-9` has `$120.00` merchandise and `$8.00` shipping. It enters `SAVE20`.

| Campaign | Policy | Remaining before commit |
| --- | --- | ---: |
| `SAVE20-v3` | `$20` off at `$100` minimum; priority 80; group `ORDER`; entered code | 1 global use |
| `AUTO10-v7` | 10% off merchandise; priority 50; group `ORDER` | Unlimited |
| `SHIP-v2` | Free shipping; priority 20; group `SHIPPING` | Unlimited |

1. Preview shortlists and sorts the versions. `SAVE20-v3` applies for `$20.00`;
   `AUTO10-v7` is rejected with `STACKING_GROUP_OCCUPIED`; `SHIP-v2` applies `$8.00`.
   Quote `Q-4` records the cart fingerprint and totals `$128 - $28 = $100`.
2. Orders `O-42` and `O-43`, both holding valid previews, concurrently request the last
   `SAVE20-v3` use. Stable locking lets `O-42` commit `R-42`; `O-43` receives
   `LIMIT_EXHAUSTED` with no partial counter change and reprices to `AUTO10 + SHIP`.
3. The response for `O-42` is lost. Retrying its original idempotency key returns `R-42`
   and `$100`, with `replayed=true`; the coupon count remains one.
4. If `O-42` is later cancelled, one allowed reversal changes `R-42` to `REVERSED` and
   restores the use. A duplicate reversal returns the first reversal result.

### Expected solution and design-principle reasoning

The expected shape is a pure evaluator around immutable `CampaignVersion`, composable
`Condition`, pluggable `Benefit`, and a deterministic `StackingPolicy`, with a separate
transactional `RedemptionLedger`. A catalog/repository supplies candidates; a clock and
facts are explicit inputs. This keeps quote calculation independently testable while
placing every usage mutation behind one consistency boundary.

| Principle or pattern | Justified use | Why it fits here |
| --- | --- | --- |
| SRP | Separate definition/catalog, evaluation, selection, money allocation, and redemption | Campaign editing, arithmetic, and concurrency change for different reasons |
| OCP + Strategy | `Benefit` strategies for percentage, fixed, BOGO, and shipping; `StackingPolicy` for selection | New benefit or policy behavior is added without editing the orchestration pipeline |
| Specification/Composite | Leaf conditions combine with `all/any/not` and return structured failures | Eligibility is composable and explainable; boolean-only predicates would lose reasons |
| DIP | Evaluator depends on catalog/ledger interfaces and injected clock, not SQL or global time | Enables in-memory unit tests and production adapters with identical domain behavior |
| ISP | Split read-only campaign lookup, pure evaluation, and redemption commands | Preview callers cannot accidentally acquire quota-mutating capabilities |
| LSP | Every condition/benefit honors currency, non-negative, deterministic result contracts | A new implementation cannot silently weaken pricing invariants |
| Repository + Unit of Work | Persist versions and atomically commit ledger/counters/idempotency | The pattern expresses the real transaction boundary; it is not hidden in domain entities |

Rejected alternatives: a growing coupon `if/switch`; mutable coupons embedded as cart
items; binary floating-point money; a generic Chain of Responsibility whose mutation
order is implicit; brute-force subset optimization; in-memory locks as the production
limit mechanism; and a full Interpreter/DSL before rule-authoring needs are known. A
State-pattern class hierarchy is also unnecessary while campaign transitions remain a
small guarded table—an enum plus transition policy is clearer.

Test with table-driven rules, property tests for money/allocation invariants, golden
quotes for deterministic ordering, injected-clock boundary tests, strategy contract
tests, request-hash idempotency tests, and a barrier-controlled last-use race. Extending
conditions or benefits requires a new implementation and registration, not changes to
redemption or existing strategies.

### Senior and Staff expectations

| Decision | Strong Senior answer | Staff-level extension |
| --- | --- | --- |
| Consistency | One transactional ledger is authoritative; previews are advisory | State explicit consistency/SLO bounds and keep usage writes single-region or partition-owned |
| Campaign lookup | Coarse indexes/cache build a bounded shortlist; evaluator remains pure | Versioned cache invalidation, tenant isolation, rollout/canary, and audit provenance |
| Hot global limits | Row lock/conditional counter is correct first | Partition traffic, queue scarce claims, or use reservation tokens; explain fairness and hot-key cost |
| Checkout latency | Bounded candidate count and no remote calls inside commit | Precompute facts, measure p95/p99 by rule, and shed optional promotions without corrupting totals |
| Long checkout | Consume near order placement and compensate on cancellation | Introduce expiring reservations only with explicit `RESERVED/COMMITTED/EXPIRED` semantics |
| Multi-region | Replicate definitions/read models; route redemption to an owner | Do not claim globally strict counters from eventually consistent replicas; choose regional quotas or coordination |
| Optimization | Deterministic groups and priorities are explainable | Add a bounded optimizer only with a formal objective, dependency graph, complexity limit, and explanation trace |
| Operations | Structured rejection and ledger audit | Outbox analytics, anomaly alarms, replay tooling, kill switches, and reconciliation of counters to ledger |

A Senior solution is complete when correctness is demonstrable on one durable store. A
Staff solution additionally identifies hot keys, regional ownership, operability, and a
safe migration path without weakening the core invariants.

### Interviewer follow-ups with concise strong answers

| Follow-up | Strong answer |
| --- | --- |
| Why can preview and checkout disagree? | Time and quota can change. Preview snapshots versions/facts for explanation; redemption revalidates under the quota transaction and returns `REPRICE_REQUIRED` or `LIMIT_EXHAUSTED`. |
| How do you stop the final coupon being used twice? | Lock or conditionally increment the same authoritative counter row, insert a unique ledger record, and commit counter plus redemption atomically. |
| The server committed but the response was lost—what now? | Retry the same idempotency key. The stored request hash/result proves whether it committed; never infer failure from a timeout. |
| Why store a request hash with the key? | A key replay is safe only for the same intent. A different cart/order under the same key is a client bug and must conflict, not return an unrelated discount. |
| What if two promotions need counters and workers lock them in different orders? | Acquire by stable campaign-version ID inside one transaction; retry only genuine serialization/deadlock aborts. |
| What happens when the cart changes after preview? | Its canonical fingerprint changes, so redemption rejects the quote and reprices. Never apply a quote to a different quantity or SKU set. |
| How do you edit a live campaign safely? | Publish a new immutable version. Existing quotes/redemptions retain old version IDs; discovery switches versions at a declared instant. |
| How do you allocate a one-cent remainder? | Compute in minor units, floor proportional shares, then distribute residual cents in a stable line-ID order. The allocations must sum to the parent discount. |
| Why not always choose the maximum customer savings? | Arbitrary stacking dependencies create an expensive, unstable search. Use a documented bounded policy unless “maximize discount” is a paid-for requirement with a formal tie-breaker. |
| Can Redis alone enforce the quota? | It can be an optimization, but atomic counters without the durable order/redemption/idempotency transaction create reconciliation gaps. The ledger remains authoritative. |
| What does cancellation do after the campaign has ended? | It follows the snapshotted version's reversal policy and appends one reversal; current eligibility dates do not rewrite history. |
| How would you test the race deterministically? | Start two transactions at a barrier against a limit of one, assert exactly one commit, then replay both keys and reconcile ledger rows to counters. |

### Red flags

- Starts with subclasses or a pattern catalog before defining stacking and commit semantics.
- Mutates cart prices during evaluation, uses `double`, or cannot reconcile line totals.
- Lets repository iteration order decide winners or reports only “invalid coupon.”
- Consumes quota during preview or updates global and customer counters separately.
- Uses an idempotency key without scope, request hash, uniqueness, and stored outcome.
- Claims an HTTP timeout means redemption failed, or retries validation failures blindly.
- Uses process-local synchronization for a multi-instance service.
- Edits published campaigns in place or reads mutable definitions during a retry.
- Calls inventory/customer services while holding quota locks.
- Reaches for a universal DSL or exponential optimizer without a motivating requirement.

### Scoring rubric

| Area | Points | Evidence for full credit |
| --- | ---: | --- |
| Clarification and scope | 10 | Separates preview/redemption, states stacking, money, limits, and exclusions |
| Domain model and ownership | 15 | Immutable versions, pure rules/benefits, quote, ledger, and clean external boundaries |
| APIs and observable contracts | 15 | Explained quote, stale/conflict errors, request-hash idempotency, query/reversal |
| Pricing invariants and determinism | 15 | Exact money, stable ordering/allocation, caps, group/exclusive semantics |
| Concurrency and failure correctness | 25 | Atomic multi-counter commit, final-use race, stable locks, retry and unknown-outcome handling |
| Design reasoning and testability | 10 | Justified SOLID/patterns, rejected alternatives, deterministic/race tests |
| Trade-offs and evolution | 10 | Hot keys, reservations, multi-region ownership, operations, safe migration |
| **Total** | **100** | |

`70+` with no gap in money, atomicity, or idempotency is a Senior pass. `85+` plus
explicit hot-key/regional/operability trade-offs and an evolutionary plan is a Staff
signal. A polished class diagram cannot compensate for a broken redemption boundary.

### Interview variations and expected solutions

The variants below turn every distinct relevant source into an interview exercise.
They summarize ideas rather than reproduce source code or prose; the primer and aggregate
Kumar snapshot have no repository-wide license. “Canonical runnable parts” refers to the
Java 17 solution documented later in this page.

#### Variation 1 — Generic coupon/promocode eligibility and usage

- **Candidate prompt / scope delta:** Design coupon codes that support capped percentage
  and flat discounts, optional minimum cart value, one/few/all customers and merchants,
  and one/few/unlimited uses. Automatic promotions, BOGO, free shipping, and arbitrary
  stacking are optional; make the usage boundary explicit.
- **Expected Senior solution:** Model immutable coupon versions with composable customer,
  merchant, subtotal, and time specifications; exact `Money`; fixed/capped-percentage
  benefit strategies; a pure eligibility/price preview; and an idempotent transactional
  ledger for global and per-customer uses. Define stable tie-breaking if more than one
  entered code is allowed.
- **Staff extension / trade-offs:** Add merchant-funded budget ledgers, tenant isolation,
  hot-code contention controls, fraud signals outside the pricing transaction, regional
  ownership, counter reconciliation, and an explicit decision between one-code checkout
  simplicity and bounded best-value selection.
- **Canonical runnable fit:** Reuse `Money`, subtotal/customer `Condition`s, capped
  percentage/fixed `Benefit`s, explained evaluation, `UsagePolicy`, and atomic redemption.
  Add merchant facts/conditions and merchant-budget counters; disable unused automatic,
  BOGO, shipping, and stacking features rather than forking the pipeline.

**Provenance:** **Requirements-only prompt**, summarized from [local
`questions.md`, lines 168–174](../../References/low-level-design-primer/questions.md) and
[coverage row 089](../../References/PRIMER_QUESTION_INDEX.md); [pinned upstream at
`49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L168-L174).

#### Variation 2 — Sequence-sensitive coupons inside a heterogeneous cart

- **Candidate prompt / scope delta:** A cart contains items and coupon tokens in order.
  Support a percentage over all items, a percentage on the next item after a token, and
  a fixed discount on the Nth later item of a category. Sequence is business data; focus
  on class design and repeatable total calculation, not campaign administration.
- **Expected Senior solution:** Preserve an immutable ordered cart snapshot, but represent
  coupon tokens separately from sellable items through a sealed entry type or tagged
  value. Each positional rule returns adjustments against stable item IDs; a coordinator
  applies them once in declared order using exact money and prevents negative totals.
  Repeated `total()` calls must be pure—never mutate item prices.
- **Staff extension / trade-offs:** Define token removal/reordering semantics, coupon
  compatibility, audit explanations, large-cart complexity, and whether tokens are
  customer-visible commands or merely a UI projection over normal campaign definitions.
  Add versioning/redemption only if these tokens consume scarce uses.
- **Canonical runnable fit:** Reuse `Money`, `PricingState`, adjustment breakdowns, caps,
  and deterministic evaluation. Adapt `EvaluationContext` to an ordered entry stream and
  add positional conditions/benefits. Do **not** make canonical `CartLine` mutable or make
  `Coupon` inherit from `Product`; the existing ledger is optional for an in-memory-only
  exercise.

**Provenance:** **Runnable plain-source variation**, summarized from the [local Kumar
`Coupon` tree](../../References/kumaransg-LLD/Low_level_Design_Problems/Coupon/) and
[variation index](../../References/VARIATION_INDEX.md); [pinned upstream at
`1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Coupon).
The index's “Coupon cart” entry points to the same subtree, so it is a **duplicate
attachment**, not another variation.

#### Variation 3 — Product offer index and deterministic best-offer lookup

- **Candidate prompt / scope delta:** Add and remove priced offers for a product and
  return the best eligible offer for a requested price, with a defined meaning of “best”
  and deterministic ties. This is an offer-index data-structure problem, not a coupon
  lifecycle or redemption system.
- **Expected Senior solution:** Clarify whether best means lowest payable price or nearest
  price not exceeding a target. Use `Map<ProductId, NavigableMap<Money, SortedSet<OfferId>>>`
  plus `Map<OfferId, ProductId/Price>` for efficient removal; return `Optional`, validate
  duplicates, use exact money, and tie-break by offer ID. Protect compound updates with
  one lock or repository transaction.
- **Staff extension / trade-offs:** Discuss concurrent readers/writers, snapshot/version
  semantics, memory bounds, persisted secondary indexes, partitioning by product, and
  stale-read tolerance. Do not introduce global promotion optimization to solve a local
  ordered-index query.
- **Canonical runnable fit:** Reuse the `Money` value object and deterministic-selection
  discipline only. `Promotion`, composable conditions, `PricingState`, and redemption are
  intentionally too broad unless the prompt later adds eligibility and use limits.

**Provenance:** **Minimal fragment**, summarized from the [local
`systemdesign/shoppingcart` tree](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/shoppingcart/)
listed in the [variation index](../../References/VARIATION_INDEX.md); [pinned upstream at
`1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/shoppingcart).
It is not a complete coupon solution.

#### Variation 4 — Ticket-booking coupons from several applicability scopes

- **Candidate prompt / scope delta:** A booking may receive coupons attached to the user,
  movie, show, or cinema. Find eligible candidates, compare fixed versus percentage value
  at the booking price, and choose one stable best coupon. Integrate with booking without
  duplicating the same coupon discovered through several scopes.
- **Expected Senior solution:** Normalize candidates by immutable coupon/version ID,
  express each attachment scope as eligibility facts, filter expiry/usage before value
  comparison, compute exact monetary benefit, and select by discount descending then
  explicit priority/ID. Snapshot the chosen version on the booking and redeem atomically
  with booking confirmation if limits matter.
- **Staff extension / trade-offs:** Add event/show hot-key handling, cacheable candidate
  indexes versus authoritative validation, organizer/cinema funding attribution, refunds,
  regional booking ownership, and a bounded policy if multiple coupon categories may
  stack.
- **Canonical runnable fit:** Reuse `Money`, versioned `Promotion`, `Condition`, fixed and
  percentage `Benefits`, explanations, and the ledger. Add movie/show/cinema facts and a
  best-value `StackingPolicy`; connect redemption to the booking transaction or saga.

**Provenance:** **Embedded domain fragment**, summarized from the [local BookMyShow
coupon services](../../References/kumaransg-LLD/Low_level_Design_Problems/bookmyshow/src/main/java/lowleveldesign/bookmyshow/api/core/services/)
and [coupon entity](../../References/kumaransg-LLD/Low_level_Design_Problems/bookmyshow/src/main/java/lowleveldesign/bookmyshow/api/core/entities/Coupon.java);
[pinned upstream at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/bookmyshow/src/main/java/lowleveldesign/bookmyshow/api/core).
It is not a standalone promotion engine.

#### Variation 5 — Food-delivery billing selected by coupon strategy

- **Candidate prompt / scope delta:** Given a restaurant cart and one code, calculate a
  bill using either a fixed discount or percentage discount and then tax the discounted
  amount. Make it easy to add another pricing rule. Stacking and scarce coupon inventory
  are not required unless the interviewer adds them.
- **Expected Senior solution:** Separate subtotal collection, coupon eligibility, benefit
  calculation, tax policy, and bill assembly. Register strategies by a stable coupon/rule
  identifier; return an explained validation failure instead of `findAny().get()`; cap at
  zero; and use `Money` plus a declared discount-before-tax rule.
- **Staff extension / trade-offs:** Version pricing/tax policies, add restaurant and user
  scopes, decide where tax jurisdiction is owned, support audit/replay of old bills, and
  extract redemption only when limited codes or order retries make it necessary.
- **Canonical runnable fit:** Reuse `Money`, fixed/percentage `Benefits`, condition
  composition, and the adjustment breakdown. The full stacking pipeline and ledger can
  stay disabled for the simple prompt; add restaurant facts and a tax-service boundary
  rather than putting tax inside a benefit.

**Provenance:** **Embedded Strategy fragment**, summarized from the [local food-delivery
pricing strategies](../../References/kumaransg-LLD/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/src/main/java/com/mayank/fooddelivery/strategy/)
and [pricing service](../../References/kumaransg-LLD/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/src/main/java/com/mayank/fooddelivery/services/PricingService.java);
[pinned upstream at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/src/main/java/com/mayank/fooddelivery).

#### Variation 6 — Add promotional pricing to an online-shopping service

- **Candidate prompt / scope delta:** An existing shopping service already owns users,
  catalog, cart, orders, and payment. Add discounts/coupons without turning the order
  service into a campaign engine. The source gives no coupon rules, so clarification and
  an incremental boundary are the main evaluation criteria.
- **Expected Senior solution:** Start with an in-process `PricingPolicy` boundary over an
  immutable cart snapshot, exact adjustment lines, and a quote fingerprint; keep order
  totals derived from the quote. If codes have limits, introduce idempotent redemption
  tied to order placement rather than updating counters from cart preview.
- **Staff extension / trade-offs:** Extract the engine only when independent campaign
  release cadence, tenant reuse, scale, or hot quotas justify the network boundary.
  Define API/version/SLO ownership, failure behavior at checkout, and migration that
  preserves quote and redemption IDs.
- **Canonical runnable fit:** The canonical evaluator, explanations, and redemption model
  are the target extracted capability. Adapt its inputs to shopping cart/catalog facts and
  its commit call to the order workflow; payment consumes the final quoted payable amount
  but never owns promotion state.

**Provenance:** **Requirements-only fragment**, summarized from the [local awesome
online-shopping README](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/README.md);
[pinned upstream at `fc26e40`, lines 96–100](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/onlineshoppingservice/README.md#L96-L100).
That GPL-3.0 snapshot contains no dedicated coupon implementation; its other search hit
uses “promotion” for a course waitlist and is unrelated to promotional pricing.

## Functional Requirements

1. Create and manage promotions with validity windows and lifecycle state.
2. Support automatic promotions and customer-entered coupon codes.
3. Evaluate eligibility using cart total, SKU, category, quantity, customer segment,
   sales channel, and other composable facts.
4. Support percentage discounts, fixed discounts, buy-X-get-Y, and free shipping.
5. Model priorities, exclusive promotions, stacking groups, caps, and minimum spend.
6. Return applied and rejected promotions with human-readable reasons.
7. Separate preview/evaluation from redemption.
8. Enforce global and per-customer redemption limits atomically and idempotently.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `Promotion` | Versioned campaign definition and lifecycle |
| `Coupon` | Redeemable code and usage constraints |
| `Condition` | Composable eligibility rule |
| `Benefit` | Discount calculation strategy |
| `EvaluationContext` | Cart, customer, channel, and current time |
| `DiscountLine` | Applied adjustment and explanation |
| `Redemption` | Durable use of a promotion by an order/customer |
| `Money` | Exact amount and currency operations |

## Business Rules

- Define a stable evaluation order so identical inputs produce identical output.
- Do not apply a discount below zero or beyond its configured cap.
- Define how line-level and order-level discounts allocate across cart lines.
- Define currency and rounding rules explicitly; never use binary floating point for
  money.
- An inactive, expired, exhausted, or ineligible promotion must explain its rejection.
- Re-evaluating a saved order must use the relevant promotion version or snapshot.
- Redemption must be reversible when an order is cancelled, if campaign policy
  allows it.

## Concurrency and Failure Handling

- Concurrent attempts to consume the final global use must allow at most one.
- Retrying redemption for the same order and coupon must not consume another use.
- A preview must not consume quota.
- Redemption and rollback must not leave counters inconsistent after failure.

## Demonstration Scenarios

1. Apply one eligible automatic percentage discount.
2. Reject an expired or minimum-spend coupon with a reason.
3. Resolve two exclusive promotions according to priority or best value.
4. Combine compatible line-level and order-level promotions deterministically.
5. Execute buy-X-get-Y with eligible and insufficient quantities.
6. Race two redemptions for the final global use and retry the winner.

## Extensions

- A promotion rule DSL
- Product bundles and tiered pricing
- Experiment cohorts and personalised offers
- Administrative approval and scheduled publication

## Related Problems

- [Order Management System](../order_processing_system/README.md)
- [Product Catalog Service](../product_catalog_service/README.md)
- [Shopping Cart with Expiration](../shopping_cart_with_expiration/README.md)

## Reference Solution (Java 17)

**Status:** Implemented and verified. The solution compiles with `--release 17
-Xlint:all`, and its executable demo covers automatic discounts, explained coupon
rejection, exclusive priority, compatible stacking, BOGO, free shipping, a concurrent
last-use race, idempotent retry, and quota-restoring cancellation.

### Architecture

```text
EvaluationContext
      |
      v
PromotionEngine -- priority/id order --> Promotion
      |                                  |-- Condition (composable eligibility)
      |                                  `-- Benefit (pricing strategy)
      v
PricingState -- exact mutations --> DiscountLine + RejectedPromotion
      |
      `-- synchronized redemption ledger --> usage counters + Redemption snapshot
```

- `Money` stores non-negative minor units and enforces currency equality. Percentage
  multiplication uses basis points and half-up rounding without binary floating point.
- `Condition` composes with `and` and `or`; `Conditions` supplies subtotal, SKU,
  category, segment, and channel rules with human-readable failures.
- `Benefits` supplies capped percentage, fixed, buy-X-get-Y, and capped free-shipping
  strategies. Benefits compose in sequence and emit explicit line or shipping components.
- `PricingState` prevents the aggregate discount from crossing zero. Order-level
  discounts allocate in stable cart-line order; later benefits see remaining value.
- `PromotionEngine` is the one pipeline used by both preview and redemption. Preview
  changes no state; redemption evaluates and increments all counters inside one
  synchronized atomic boundary.
- The in-memory synchronization boundary represents a serializable transaction in a
  production repository. An order ID is the idempotency key; reuse with different
  inputs raises a domain exception.

### Deterministic Policy Decisions

1. Promotions sort by descending priority, then ascending promotion ID.
2. Coupon codes are case-normalized and only entered codes are evaluated; unknown
   codes receive an explicit rejection.
3. The first applied exclusive promotion blocks every lower-ranked promotion. An
   exclusive promotion cannot displace an already-applied higher-ranked promotion.
4. At most one promotion wins a named stacking group. Different groups may combine.
5. Validity windows are half-open: `[activeFrom, activeUntil)`.
6. Global and per-customer limit `0` means unlimited. Atomic redemption prevents two
   callers from consuming the final use.
7. Cancellation is all-or-nothing: every applied promotion must be reversible before
   any counter is decremented.

### Source Layout

```text
src/main/java/com/example/lld/coupon_promotion_engine/
|-- Money.java                 |-- Cart.java
|-- CartLine.java              |-- Customer.java
|-- EvaluationContext.java     |-- Eligibility.java
|-- Condition.java             |-- Conditions.java
|-- Benefit.java               |-- Benefits.java
|-- BenefitOutcome.java        |-- DiscountComponent.java
|-- PricingState.java          |-- Promotion.java
|-- UsagePolicy.java           |-- PromotionEngine.java
|-- PromotionException.java    `-- CouponEngineDemo.java
```

### Compile and Run

Run from `Problems/coupon_promotion_engine`:

```bash
mkdir -p /tmp/coupon-promotion-engine-classes
find src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all \
      -d /tmp/coupon-promotion-engine-classes
java -cp /tmp/coupon-promotion-engine-classes \
  com.example.lld.coupon_promotion_engine.CouponEngineDemo
```

Expected final line:

```text
Coupon / Promotion Engine demo: all scenarios passed
```

## Python 3 Reference Solution

**Status:** Implemented and verified using only the Python standard library.

- [`python/solution.py`](python/solution.py) is deliberately a one-hour core: immutable
  cart, promotion, quote, and redemption models; exact `Decimal` money; timezone-aware
  validity; and small `Condition`/`Benefit` protocols with minimum-subtotal, fixed, and
  percentage examples. Candidate ordering and stacking-group ownership are deterministic.
- Preview is side-effect free. A quote binds the normalized codes and checkout fingerprint
  and expires on an injected clock, no later than any applied campaign version. The
  lock-backed ledger stands in for one serializable
  transaction: it revalidates the quote, consumes global/per-customer limits, prevents a
  second redemption for an order, and stores request hashes plus results for safe replay.
  Reversible redemption is retained because releasing quota exactly once is an important
  cancellation invariant.
- [`python/test_solution.py`](python/test_solution.py) has five decisive tests: exact-cent
  rounding and discount floors, deterministic stacking, stale/conflicting requests,
  idempotent reversal, and a barrier race in which one checkout wins the final use while
  replaying the winner does not consume it twice.

### One-hour Python coding scope

**Implement live:** `Money`, cart/context, immutable `Promotion` and `Quote`; one condition
and fixed or percentage benefit policies; stable filter/sort/stack/apply pricing; and a
locked `redeem(order_id, quote, idempotency_key)` ledger. Prove exact rounding, quote
binding/expiry—including the campaign-end boundary—same-key replay/conflict, and two
threads racing for a limit of one. Add
idempotent reversal only after those invariants are green; the supplied module includes it.

**Explain rather than type:** HTTP controllers, ORM entities/SQL row locks, durable
idempotency tables, outbox/audit transport, campaign authoring and candidate indexes,
rejection-reason catalogs, line-level allocation, BOGO, shipping, exclusivity, cache
invalidation, multi-region quota ownership, fraud/budget services, a rule DSL, and the
full migration from the in-memory lock to a serializable store.

Run with Python 3.9+ and only the standard library from
`Problems/coupon_promotion_engine/python`:

```bash
python3 test_solution.py
```
