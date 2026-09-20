# Notification Framework

## Problem Description

Design a reusable framework for sending template-based notifications through email,
SMS, push, and in-app channels. The framework must respect recipient preferences and
provide reliable, observable delivery through interchangeable provider adapters.

Pub-Sub may transport a notification request, but this framework owns template
resolution, channel selection, preferences, retries, and delivery status.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design a reusable notification framework for transactional events. A caller submits
> a notification type, recipient references, template data, requested channels, priority,
> and an idempotency key. The framework resolves versioned localized templates and
> recipient preferences, then delivers through email, SMS, push, or in-app providers.
> Providers can fail synchronously, accept work asynchronously, time out with an unknown
> outcome, and send duplicate or out-of-order callbacks. Expose useful aggregate and
> per-delivery status while remaining extensible and safe under concurrent workers.

The goal is the delivery control plane, not merely an `EmailNotification` hierarchy.
The candidate should define what “sent” and “delivered” mean before selecting classes
or transport technology.

### Minute-by-minute plan

| Time | Candidate activity | Interviewer signal |
| --- | --- | --- |
| 0–5 | Clarify transactional versus marketing use, delivery semantics, fan-out, consent, and ordering | Separates business intent from provider transport |
| 5–10 | State requirements, exclusions, ownership, and SLO assumptions | Bounds a reusable framework without building a campaign platform |
| 10–20 | Model request, notification, recipient/channel delivery, immutable attempt, template, and preference | Avoids one ambiguous status for the whole request |
| 20–30 | Define submit/status/callback/provider contracts and lifecycle invariants | Makes asynchronous acceptance and partial delivery explicit |
| 30–42 | Design durable enqueue, worker claiming, retry/fallback, callback deduplication, and unknown outcomes | Does not claim impossible end-to-end exactly-once delivery |
| 42–49 | Walk the concrete scenario, including a timeout and duplicate callback | Connects state transitions to persisted evidence |
| 49–55 | Explain SOLID/pattern choices, rejected alternatives, and tests | Keeps adapters replaceable and orchestration deterministic |
| 55–60 | Evolve for fan-out, provider quotas, regions, compliance, and observability | Adds Staff concerns without weakening the core state machine |

### Clarifying questions and strong assumptions

| Ask this | Strong assumption if the interviewer leaves it open |
| --- | --- |
| Are these transactional alerts, marketing campaigns, or both? | Scope transactional notifications. Each notification type declares whether consent is required; campaign segmentation and promotional compliance workflows are separate. |
| What does `DELIVERED` mean? | It means a provider's authoritative synchronous result or verified delivery callback. HTTP/provider acceptance is `AWAITING_CALLBACK`, not delivered. |
| How many recipients can one request contain? | A normal request has a bounded list (for example, hundreds). Very large audiences submit a cohort reference to a separate fan-out job rather than a million recipients inline. |
| Are all requested channels sent? Is fallback cross-channel? | Eligible requested channels are independent logical deliveries. Provider fallback stays within a channel; email-after-push escalation requires an explicit orchestration policy and is excluded initially. |
| What do preferences and quiet hours do? | Opt-out or missing destination suppresses before any provider call. In this scope quiet hours also suppress; deferred/scheduled delivery is a later feature. Mandatory safety types may bypass only by explicit policy. |
| When are templates rendered? | Resolve locale and immutable template version during planning, validate variables, and snapshot rendered channel content so retries cannot drift after an edit. |
| Do providers support idempotency and status lookup? | Capabilities are declared per adapter. Use a stable logical delivery key where supported; otherwise document at-least-once risk and reconcile unknown outcomes before fallback. |
| Is ordering required? | No global ordering. If a caller requires order for a recipient and notification stream, it supplies a sequence key; workers then serialize that key at a throughput cost. |
| Can preferences change after submission? | Recheck revocable consent immediately before every external send. A change after provider acceptance cannot recall a message, so audit both snapshots. |
| What data may be retained? | Encrypt/tokenize destinations, redact provider payloads and errors, retain immutable delivery metadata by policy, and avoid storing secrets in audit events. |

### Scoped requirements, exclusions, and ownership boundaries

**In scope:** idempotent request acceptance; recipient/channel planning; versioned locale
resolution and rendering; consent/destination/quiet-hours checks; provider adapters and
same-channel fallback; durable asynchronous attempts; bounded retries; signed callback
handling; aggregate, per-delivery, and attempt status; audit and operational metrics.

**Explicitly out of scope:** campaign audience selection; template-authoring UI and
approval; arbitrary scheduling, batching, and digests; provider billing; inbox/read
state UI; business-event creation; mobile token registration; global message ordering;
content recommendation; guaranteeing that a human saw a message.

| Boundary | Owns | Contract with the framework |
| --- | --- | --- |
| Calling domain | Why/when to notify, recipient references, variables, type, operation key | Retries the same key after uncertainty and does not infer business success from delivery |
| Notification framework | Delivery plan, template snapshot, orchestration state, attempts, audit, aggregate view | Returns durable acceptance and observable, monotonic delivery outcomes |
| Preference/endpoint service | Consent, quiet hours, locale, verified channel destinations | Supplies versioned facts; consent is rechecked before send |
| Template registry | Immutable type/locale/channel versions and required variables | Resolution is deterministic; selected version/rendered content is snapshotted |
| Queue/scheduler | Wake-up and backpressure transport | At-least-once signals only; database state, not the queue message, is authoritative |
| Provider adapter | Vendor authentication, request translation, result/error normalization, callback mapping | Conforms to a small channel capability contract and never owns logical status |
| External provider | Transport acceptance and provider-specific receipts | May duplicate, delay, reorder, or omit callbacks; its ID is correlation, not our identity |

### APIs and contracts

```text
POST /v1/notifications                    Idempotency-Key: <tenant-scoped key>
  Request  { type, recipients[{recipientRef, channels[]}], templateData,
             locale?, priority, correlationId? }
  202      { notificationId, status: ACCEPTED, replayed }

GET  /v1/notifications/{id}
  Result   { aggregateStatus, deliveries[{recipientRef, channel, status,
             templateVersion, latestAttempt, nextAttemptAt}], attemptCursor }

GET  /v1/notifications/{id}/attempts?cursor=...
POST /v1/provider-callbacks/{provider}    signed provider event

Provider.send(ProviderRequest) -> ProviderResult
  ProviderRequest { attemptId, logicalDeliveryKey, destinationToken,
                    renderedMessage, callbackUrl, metadata }
  ProviderResult  = DELIVERED(receipt)
                  | ACCEPTED(providerMessageId)
                  | TRANSIENT_FAILURE(code, retryAfter?)
                  | PERMANENT_FAILURE(code)
                  | UNKNOWN(correlation)
```

- Submission stores `(tenantId, idempotencyKey, canonicalRequestHash)`; an equal replay
  returns the original ID, while a different payload is `409 IDEMPOTENCY_CONFLICT`.
- `202 ACCEPTED` means the framework durably owns the request, not that a provider has
  accepted or delivered it. Invalid variables or structurally invalid recipients fail
  validation before acceptance where possible.
- Provider adapters return a closed, vendor-neutral outcome taxonomy. Raw vendor codes
  remain in redacted attempt metadata; the worker, not the adapter, owns retry policy.
- Status is paginated for large fan-out. Aggregate state is derived from child delivery
  states rather than independently mutated.
- Callbacks require signature/timestamp verification, a provider plus event ID dedupe
  key, and correlation to an existing provider message/attempt. Unknown events are
  quarantined, not allowed to create notifications.

### Invariants and lifecycle

```text
Notification: ACCEPTED -> PROCESSING -> DELIVERED
                                     \-> PARTIALLY_DELIVERED
                                     \-> FAILED
                                     \-> SUPPRESSED

Delivery: PLANNED -> SUPPRESSED
                  \-> READY -> SENDING -> DELIVERED
                                         \-> AWAITING_CALLBACK
                                         \-> RETRY_WAIT -> READY
                                         \-> UNKNOWN -> reconciliation
                                         \-> FAILED
```

- One logical delivery exists per `(notificationId, recipientRef, channel)`; provider
  fallback creates a new immutable attempt under that delivery, never a new notification.
- An attempt records the provider, ordinal, rendered template version/content hash,
  timestamps, normalized result, provider ID, and redacted error. Attempts are append-only.
- Suppressed deliveries have zero provider attempts. Consent cannot be “retried.”
- A provider `ACCEPTED` result becomes `AWAITING_CALLBACK`; it is never reported as
  `DELIVERED` just to make the aggregate look successful.
- `DELIVERED` is terminal and dominates stale failures. A failure callback for an older
  attempt is audit-only; a verified success for a known attempt may complete a still
  nonterminal delivery. `FAILED` is terminal only after every outcome is confirmed.
- Aggregate status is derived: all eligible deliveries delivered is `DELIVERED`; none
  eligible and all suppressed is `SUPPRESSED`; at least one delivered plus another
  failed/suppressed is `PARTIALLY_DELIVERED`; all nonsuppressed terminal with no success
  is `FAILED`.
- The selected template version and content hash do not change across retries. Endpoint
  and revocable-consent facts may be refreshed just before an external call and audited.

### Concurrency, idempotency, retries, and unknown outcomes

Submission writes the notification, logical deliveries, idempotency record, and outbox
work in one transaction. This prevents “accepted but never enqueued.” Duplicate broker
signals are harmless because workers claim authoritative due rows.

Workers use a compare-and-set status/version or `FOR UPDATE SKIP LOCKED` lease. Before
calling a provider, a worker persists a unique attempt and stable provider idempotency
token. A second worker cannot own the same delivery lease; an expired lease is recoverable
after a crash. Rate limiting changes `nextAttemptAt`, not thread sleeps.

Retry only normalized transient failures with capped exponential backoff, jitter, and
provider `Retry-After`; permanent address/content/auth failures terminate or route to
operator action. Fallback begins only after the current provider is **confirmed** failed
or its retry budget is exhausted. Providers are selected through configured health/cost
policy, but historical attempts retain the provider actually used.

The dangerous interval is “provider may have accepted, but our call timed out.” Mark it
`UNKNOWN`; first query by provider ID/idempotency token or wait for callback. If the
provider supports idempotent send, retry the same token. If it supports neither lookup
nor idempotency, exactly-once external delivery is impossible: wait a policy-defined
uncertainty window, then choose explicitly between possible loss and possible duplicate.
Never fail over immediately and pretend the first send did not happen.

Callbacks are deduplicated by `(provider, eventId)`, verified, and applied with a
monotonic compare-and-set. Out-of-order failure cannot regress delivered state. A callback
transaction stores the event and transition atomically; downstream status events use an
outbox. Request idempotency, work dedupe, provider idempotency, and callback dedupe solve
different replay boundaries and are all needed.

### Concrete scenario walkthrough

1. The order service submits type `ORDER_DELAYED` for user `U-7`, channels `PUSH` and
   `EMAIL`, locale `en-IN`, with key `order:O-42:delayed:v1`. The transaction creates
   notification `N-77` and two logical deliveries.
2. Preferences allow push but opt out of this email type. Email becomes `SUPPRESSED`
   with reason `RECIPIENT_OPT_OUT` and no attempt. Push snapshots template `v12` and
   content hash `h12`, then becomes `READY`.
3. Push provider A is called with logical key `N-77/U-7/PUSH`; the connection times out.
   Attempt 1 and the delivery become `UNKNOWN`, and no fallback is sent. Reconciliation
   queries A by the same token and receives a confirmed “not accepted,” so retry is safe.
4. Attempt 2 receives a confirmed transient `503`; A's budget is now exhausted. Provider
   B accepts attempt 3 as message `P-900`, moving the delivery to `AWAITING_CALLBACK`.
5. B sends signed delivered event `E-51` twice. The first completes push; the second hits
   the callback dedupe record and makes no transition. The aggregate is
   `PARTIALLY_DELIVERED` because push delivered and email was intentionally suppressed.
6. A replay of the original submission returns `N-77`, including the same two logical
   deliveries. No template is rerendered and no additional provider call is created.

If provider A had offered neither lookup nor idempotent send, step 3 would remain an
explicit uncertainty/product-policy decision; provider B could not make that ambiguity
disappear.

### Expected solution and design-principle reasoning

The expected shape has a pure `NotificationPlanner` that resolves templates,
preferences, and requested channels into logical deliveries; a durable aggregate and
append-only attempts; a `DeliveryWorker` that owns the transition/retry/fallback policy;
small provider adapters; and repositories plus an outbox around atomic state changes.
Rendering and selection are independently testable; external I/O occurs only after a
durable attempt has been recorded.

| Principle or pattern | Justified use | Why it fits here |
| --- | --- | --- |
| SRP | Split planning/rendering, persistence/state transitions, worker orchestration, and vendor translation | Templates, reliability policy, and SDKs change independently |
| OCP + Strategy | Channel-selection, retry, and provider-routing policies are replaceable | Adding a provider or policy does not add branches to the state machine coordinator |
| Adapter | One adapter normalizes each vendor's requests, receipts, callbacks, and errors | Vendor DTOs and exception taxonomies do not leak into the domain |
| DIP | Planner/worker depend on provider, preference, template, repository, and clock interfaces | Fake providers and clocks exercise every branch without network or sleeping |
| ISP | Separate `ProviderSender`, optional `StatusLookup`, and `CallbackNormalizer` capabilities | A send-only provider is not forced to fake reconciliation support |
| LSP | Every adapter honors the closed outcome taxonomy and stable-key semantics it advertises | Routing can substitute providers without changing lifecycle meaning |
| Explicit state machine | Central guarded transition table rather than setters on status | Duplicate/out-of-order work has one reviewable monotonicity policy |
| Transactional outbox + Repository/Unit of Work | Atomically persist accepted work and later state events | At-least-once transport cannot create a lost notification or become source of truth |

A registry/factory may choose an adapter by `(channel, providerId)`, but a hierarchy of
`EmailNotification`, `SmsNotification`, and every vendor/channel combination is rejected:
channel content and vendor transport are separate axes. Also reject a controller that
calls SDKs directly, unbounded `sleep` retries, rendering again on every attempt, treating
provider 202 as delivered, and using Observer/Pub-Sub alone as the reliability model.
Observer is useful for local domain events; it does not persist work or resolve unknown
network outcomes. A class-per-state State pattern is unnecessary unless transitions gain
substantial state-specific behavior; a guarded table is easier to audit here.

Test pure locale/template/preference planning, transition-table properties, adapter
contract suites, retry schedules with an injected clock, callback permutations, lease
expiry, duplicate queue work, same-key/different-payload conflicts, and a scripted
timeout-before-success scenario. Extensibility means registering a conforming adapter or
policy and passing the same contracts—not editing a central channel switch.

### Senior and Staff expectations

| Decision | Strong Senior answer | Staff-level extension |
| --- | --- | --- |
| Delivery guarantee | Durable at-least-once orchestration plus provider idempotency where available | Publish a guarantee matrix per provider/channel and build unknown-outcome reconciliation |
| Persistence/queue | Database aggregate + outbox + leased workers is a correct baseline | Partition by tenant/channel, isolate noisy neighbors, and state recovery-point objectives |
| Fan-out | One child row per recipient/channel makes partial state explicit | Chunk cohort expansion, stream status pages, control write amplification and hot tenants |
| Provider routing | Ordered fallback after confirmed failure | Health/cost/region-aware policy, circuit breakers, canaries, and per-provider rate budgets |
| Preferences | Resolve and recheck consent before send | Regional policy engines, immutable legal evidence, deletion/retention workflows |
| Templates | Immutable version and rendered snapshot | Approval/rollout, experiment lineage, schema compatibility, and emergency rollback |
| Multi-region | Give each notification/delivery one write owner | Region-local endpoints/data residency, replicated read views, callback routing to owner |
| Operations | Attempts, audit, metrics, and dead-letter inspection | SLOs for accept/send/callback lag, provider attribution, replay/reconcile tooling, cost controls |

A Senior pass requires honest failure semantics and a coherent single-region durable
design. Staff signal comes from controlling fan-out and noisy neighbors, capability-aware
provider guarantees, compliance/data residency, observability, and an incremental path
that preserves identifiers and lifecycle invariants.

### Interviewer follow-ups with concise strong answers

| Follow-up | Strong answer |
| --- | --- |
| The provider returned `202`; is the notification delivered? | No. It is `AWAITING_CALLBACK` or provider-accepted. Only an authoritative synchronous delivery result or verified delivery receipt makes it `DELIVERED`. |
| The send timed out. Why not retry immediately? | The outcome is unknown and an immediate retry/fallback can duplicate. Query or retry the same provider idempotency token; otherwise expose the policy trade-off. |
| Can you guarantee exactly once? | Not end to end when an external provider lacks idempotency/status lookup and the network can fail. We provide durable at-least-once orchestration and effectively-once where capabilities permit. |
| How do two workers avoid sending the same delivery? | Claim with versioned CAS/lease, persist the attempt before I/O, and let only the lease owner advance it. Lease expiry handles crashes; the provider token handles the post-send crash window. |
| Why are there both deliveries and attempts? | A delivery is the recipient/channel obligation; attempts are immutable evidence of retries and providers. Fallback changes attempts, not the obligation's identity. |
| How are duplicate and out-of-order callbacks handled? | Verify, insert a unique provider/event ID, correlate the attempt, then apply one monotonic transition. Stale failure is audit-only and cannot undo success. |
| What if the user opts out between planning and retry? | Recheck revocable consent just before the next external send and suppress it. Preserve both preference versions for audit; already accepted sends cannot be recalled. |
| Why snapshot rendered content? | A retry must represent the same accepted intent. Rerendering after a template edit can send recipients different facts under one notification ID. |
| When should provider fallback occur? | After confirmed permanent failure or exhausted transient budget—not during an unresolved timeout. Permanent invalid destinations do not become valid at another provider. |
| What if enqueue succeeds but the database transaction rolls back? | Consumers find no due authoritative row and do nothing. More importantly, the outbox publishes only committed work, so accepted rows cannot miss enqueue. |
| How is aggregate partial status computed? | Derive it from logical deliveries. At least one success plus another failed/suppressed is partial; attempts never directly set aggregate status. |
| How would you send to ten million users? | Submit a cohort/campaign job, expand in bounded chunks, create independently claimable deliveries, enforce tenant/provider quotas, and page aggregates; do not put ten million recipients in one transaction. |
| Does priority guarantee ordering? | No; it affects scheduling fairness. Ordering needs an explicit sequence key and serialized consumer path, with documented throughput and head-of-line blocking. |
| How do you test without sleeping or real providers? | Inject clock/scheduler and scripted adapters, advance time deterministically, permute callbacks, expire leases, and assert the transition and attempt history. |

### Red flags

- Models only `Notification.send()` subclasses and has no logical delivery or attempt.
- Treats Pub-Sub/Observer as proof of durable or exactly-once notification delivery.
- Reports provider acceptance as delivery or has one status that hides partial outcomes.
- Retries every exception, sleeps worker threads, or ignores rate-limit `Retry-After`.
- Immediately fails over after a timeout without naming the duplicate risk.
- Stores idempotency keys without tenant scope, request hash, uniqueness, or result.
- Lets two workers send before atomically claiming and recording an attempt.
- Rerenders on retry, so one notification ID can produce changing content.
- Allows a stale callback to regress a terminal success or trusts unsigned callbacks.
- Couples vendor SDK objects/errors to the public API or core state machine.
- Sends despite opt-out because preferences were checked only at initial submission.
- Stores raw destinations, message bodies, or provider secrets in unrestricted audit logs.

### Scoring rubric

| Area | Points | Evidence for full credit |
| --- | ---: | --- |
| Clarification and scope | 10 | Defines audience, channel policy, delivery meaning, consent, fan-out, and exclusions |
| Domain model and ownership | 15 | Separates request, logical delivery, immutable attempt, provider receipt, and external owners |
| APIs and lifecycle contracts | 15 | Durable `202`, detailed status, provider outcomes, signed callbacks, monotonic aggregate rules |
| Extensibility and design reasoning | 15 | Justified Adapter/Strategy/DIP/ISP, rejected alternatives, contract-test seam |
| Concurrency and failure correctness | 25 | Outbox, worker lease/CAS, bounded retry, callback dedupe, unknown-outcome honesty |
| Scenario and testability | 10 | Traces state/evidence and proposes deterministic retry/callback/race tests |
| Staff trade-offs and evolution | 10 | Fan-out, noisy neighbors, provider capabilities, regions, compliance, SLO/tooling |
| **Total** | **100** | |

`70+` with no gap in consent, durable acceptance, monotonic state, or unknown-outcome
handling is a Senior pass. `85+` plus concrete fan-out isolation, guarantee matrix,
regional/compliance ownership, and operational evolution is a Staff signal.

### Interview variations and expected solutions

The variants below convert every distinct relevant prompt or implementation family into
an interview exercise. Language ports and equivalent skeletons are collapsed rather
than counted as separate designs. The primer and aggregate Kumar snapshot have no
repository-wide license, so their ideas are summarized rather than copied. “Canonical
runnable parts” refers to the Java 17 solution documented later in this page.

#### Variation 1 — Promotion-event fan-out to mobile and email

- **Candidate prompt / scope delta:** Receive an event from a promotions team and notify
  registered users through iOS push, Android push, email, or a configured combination.
  Clarify whether this is a small transactional audience or a bulk marketing campaign;
  the variant adds event ingestion and platform-specific push routing.
- **Expected Senior solution:** Persist/deduplicate the source event, resolve recipient
  endpoints and consent, expand one logical delivery per eligible user/channel, snapshot
  content, and dispatch asynchronously through adapters. Treat iOS and Android as push
  provider/platform capabilities under one `PUSH` channel unless their content contracts
  differ. Expose per-delivery status and partial results.
- **Staff extension / trade-offs:** For bulk campaigns, split audience selection into a
  cohort service, stream bounded fan-out chunks, isolate tenants, enforce provider and
  recipient rate limits, prioritize transactional traffic, and define unsubscribe/legal
  evidence, cost budgets, and lag SLOs. At-least-once event ingestion needs a stable
  campaign-event key.
- **Canonical runnable fit:** Reuse request idempotency, preferences, templates, logical
  channel deliveries, provider chain, retry, callback dedupe, and status history. Adapt
  submission with a promotion-event consumer/outbox and add platform/token metadata;
  replace inline recipient lists with chunked cohort expansion for large audiences.

**Provenance:** **Requirements-only prompt**, summarized from [local `questions.md`,
lines 48–51](../../References/low-level-design-primer/questions.md) and [coverage row
016](../../References/PRIMER_QUESTION_INDEX.md); [pinned upstream at
`49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L48-L51).

#### Variation 2 — Multi-recipient Swiggy notifications across user roles

- **Candidate prompt / scope delta:** Given a message and a list containing delivery
  partners, employees, and app customers, notify each recipient. The key ambiguity is
  whether role changes content/channel policy or only how an endpoint is resolved; this
  is a bounded multi-recipient service rather than a promotions event pipeline.
- **Expected Senior solution:** Use `RecipientRef` plus a role-aware resolver, not role
  subclasses with `send()`. Validate and snapshot a delivery plan, apply per-type consent
  and priority policy, create independently observable recipient/channel deliveries, and
  process them asynchronously with idempotent submission and bounded concurrency.
- **Staff extension / trade-offs:** Add role-specific criticality and escalation, noisy-
  neighbor fairness, chunked fan-out, workforce-versus-customer data boundaries, regional
  endpoint ownership, and separate SLOs for operational alerts and consumer messages.
  Discuss whether one aggregate across millions is useful or only a campaign summary is.
- **Canonical runnable fit:** Reuse the planner, templates, preferences, provider adapters,
  retry state machine, and aggregate/attempt view. Add typed recipient resolution and
  paginated/chunked delivery creation; the canonical recipient object should remain a
  value/facts carrier, not a polymorphic sender.

**Provenance:** **Distinct requirements-only prompt**, summarized from [local
`questions.md`, lines 246–247](../../References/low-level-design-primer/questions.md) and
[coverage row 127](../../References/PRIMER_QUESTION_INDEX.md); [pinned upstream at
`49fe9f2`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L246-L247).
It is not a duplicate of the promotion-event prompt.

#### Variation 3 — Replace a channel switch with pluggable senders

- **Candidate prompt / scope delta:** Refactor a service that branches on `EMAIL`, `SMS`,
  or `PUSH` and directly constructs senders. Make adding a channel or implementation safe
  and testable. Persistence, templates, preferences, retry, and callbacks are deliberately
  outside this small pattern exercise unless requested.
- **Expected Senior solution:** Define a narrow `ChannelSender` or provider port, implement
  one adapter per transport, and inject a registry keyed by channel/provider. A simple
  factory/registry is sufficient; validate unknown channels and return a typed outcome.
  Keep message content separate from transport construction and avoid a singleton.
- **Staff extension / trade-offs:** Evolve the port into capability interfaces—send,
  idempotency, receipt lookup, callback normalization—and select providers by policy
  without leaking vendor DTOs. Resist turning a five-class refactor into a distributed
  platform before reliability requirements exist.
- **Canonical runnable fit:** Reuse `ChannelProvider`, provider registration/fallback,
  normalized failures, and scripted adapter contract tests. The canonical aggregate and
  retry lifecycle are optional for the narrow kata; when reliability is added, the
  registry plugs into the existing worker rather than replacing its state machine.

**Provenance:** **Pattern fragment with language-port duplicates**, represented by the
[local Java factory example](../../References/awesome-low-level-design/design-patterns/java/factory/notification/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/java/factory/notification)).
The equivalent [C++](../../References/awesome-low-level-design/design-patterns/cpp/factory/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/cpp/factory)),
[C#](../../References/awesome-low-level-design/design-patterns/csharp/factory/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/csharp/factory)),
[Go](../../References/awesome-low-level-design/design-patterns/golang/factory/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/golang/factory)),
and [Python](../../References/awesome-low-level-design/design-patterns/python/factory/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/python/factory))
trees are collapsed; they are not five architectural variants.

#### Variation 4 — In-app social notifications through domain events

- **Candidate prompt / scope delta:** When a connection request, follow, like, comment,
  or similar social action occurs, create an in-app notification and let the member list
  unread items. This is a durable inbox/read-state problem; email/SMS provider delivery
  and retry may be excluded.
- **Expected Senior solution:** Publish a typed domain event after the source transaction,
  consume it idempotently into an immutable notification/inbox record, and model recipient,
  type, content reference, created time, and independent `readAt`. Use Observer only for
  in-process decoupling; use an outbox/consumer for durability. Enforce authorization and
  stable event-to-notification dedupe.
- **Staff extension / trade-offs:** Add high-fan-out feed strategies, websocket wake-ups,
  unread counters with reconciliation, event ordering, retention/deletion, abuse controls,
  and a policy for promoting selected in-app events to external channels. Separate
  content/feed state from external delivery receipts.
- **Canonical runnable fit:** Reuse notification identity, template rendering, request
  dedupe, and the `IN_APP` channel concept. Adapt the provider to a transactional inbox
  repository and add `readAt`; do not equate canonical `DELIVERED` with “read.” External
  attempts/provider callbacks can be omitted until cross-channel escalation is added.

**Provenance:** **Embedded Observer/in-app fragments**, represented by the [local Java
LinkedIn slice](../../References/awesome-low-level-design/solutions/java/src/linkedin/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/linkedin))
and [local Java Observer notification fragment](../../References/awesome-low-level-design/design-patterns/java/observer/NotificationServiceNaive.java)
([pinned](https://github.com/ashishps1/awesome-low-level-design/blob/fc26e4033cad6d24f32caa8521044febbf065beb/design-patterns/java/observer/NotificationServiceNaive.java)).
The [C# LinkedIn port](../../References/awesome-low-level-design/solutions/csharp/linkedIn/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/csharp/linkedIn)),
[Go LinkedIn port](../../References/awesome-low-level-design/solutions/golang/linkedin/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/golang/linkedin)),
[Python LinkedIn port](../../References/awesome-low-level-design/solutions/python/linkedin/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/python/linkedin)),
and [Java social-network model](../../References/awesome-low-level-design/solutions/java/src/socialnetworkingservice/)
([pinned](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/socialnetworkingservice))
are **language/domain duplicates** of this small idea, not standalone delivery
frameworks; their Go/Python social-network ports are collapsed with that model.

#### Variation 5 — Scheduled hotel booking reminders over email and SMS

- **Candidate prompt / scope delta:** Send check-in and checkout alerts for a hotel
  booking through email or SMS using templates. Unlike the base guide, scheduled delivery,
  booking changes, and local time are central; the source classes themselves are stubs.
- **Expected Senior solution:** Convert booking lifecycle changes into idempotent reminder
  commands keyed by `(bookingId, reminderType, bookingRevision)`, resolve an immutable
  template and endpoint, persist `scheduledAt` in UTC with hotel/recipient timezone, and
  let due workers use the normal delivery state machine. Cancellation/reschedule must
  invalidate obsolete revisions without deleting audit history.
- **Staff extension / trade-offs:** Handle timezone/DST policy, millions of future timers,
  partitioned scheduler scans, late-job SLOs, booking-data minimization, regional provider
  routing, and escalation from SMS to email only when product semantics permit it.
- **Canonical runnable fit:** Reuse templates, preferences, email/SMS adapters, retry,
  fallback, idempotency, and attempt history. Add durable `scheduledAt`, booking revision,
  and supersession states; adapt quiet-hours suppression if a reminder should instead be
  deferred.

**Provenance:** **Embedded stub fragment**, summarized from the [local Kumar hotel
management tree](../../References/kumaransg-LLD/Low_level_Design_Problems/HotelManagmentSystem/)
and its entry in the [variation index](../../References/VARIATION_INDEX.md); [pinned
upstream at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/HotelManagmentSystem).
The index classifies the overall hotel solution, not a complete notification framework.

#### Variation 6 — Repair an abstract `send()` plus boolean-status design

- **Candidate prompt / scope delta:** A food-ordering model has an abstract notification
  with content, one boolean status, and `send()`, plus email/SMS/push types. Evolve it for
  order events while keeping the exercise local. The design review—not feature breadth—is
  the core task.
- **Expected Senior solution:** Replace inheritance-based transport with immutable
  `NotificationRequest`, channel `Delivery`, append-only `Attempt`, and injected sender
  ports. Replace the boolean with explicit states and typed outcomes; add endpoint and
  template boundaries, request idempotency, and retry only if reliability is required.
- **Staff extension / trade-offs:** Decide when a shared framework is justified across
  restaurant, courier, and customer domains; add provider capability contracts, outbox,
  consent, SLOs, and PII retention incrementally. Preserve a thin synchronous adapter if
  the actual scale/failure requirements remain small.
- **Canonical runnable fit:** The canonical request, template, preference, provider,
  retry, and status model is the expected replacement. Adapt notification types to order
  events and keep domain event creation in food ordering; little of the abstract base
  class should be retained beyond its notion of content.

**Provenance:** **Embedded abstraction fragment**, summarized from the [local Kumar
food-ordering notification tree](../../References/kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/food-ordering-system/src/com.bonvivant/service/notification/)
and [channel enum](../../References/kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/food-ordering-system/src/com.bonvivant/enums/NotificationType.java);
[pinned upstream at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/low-level-design-3/food-ordering-system/src/com.bonvivant).

#### Variation 7 — Extract notification hooks from Amazon and Stack Overflow models

- **Candidate prompt / scope delta:** Several domain models contain placeholder
  `sendNotification` methods or a stateful notification service. Design the boundary by
  which an order, answer, or account event requests a notification without coupling the
  domain transaction to an external provider. These are skeletons, not runnable framework
  candidates.
- **Expected Senior solution:** The domain writes a typed event through its transaction's
  outbox; a notification application service maps that event to a versioned type/template
  and submits it with the domain event ID as idempotency key. Keep domain IDs/correlation
  in metadata, provider details outside entities, and make delivery failure observable
  without rolling back the source business event.
- **Staff extension / trade-offs:** Define platform versus domain ownership, schema
  compatibility, per-domain SLO/priority, replay safety, multi-tenant isolation, and
  whether critical flows need synchronous acknowledgement of durable acceptance. Avoid
  a universal event schema that erases domain meaning.
- **Canonical runnable fit:** Reuse the entire canonical submission and delivery pipeline;
  add event-to-request mappers and a transactional outbox at each producer. The placeholder
  `send` methods and boolean returns should not be adapted into provider implementations.

**Provenance:** **Duplicate-like skeleton fragments**, collapsed from the [local
`amazon_new/NotificationService.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/amazon_new/NotificationService.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/amazon_new/NotificationService.java)),
[local Amazon `Notification.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/Notification.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/Notification.java)),
and [local Stack Overflow `Notification.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/stackoverflow/Notification.java)
([pinned](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/stackoverflow/Notification.java)).
They add no distinct channel or reliability implementation, so counting them separately
would overstate the source material.

## Functional Requirements

1. Accept a notification type, recipients, template data, priority, and idempotency
   key.
2. Resolve versioned, localised, channel-specific templates.
3. Respect opt-ins, opt-outs, quiet hours, and preferred channels.
4. Route each delivery through a pluggable provider adapter.
5. Support configurable fallback providers for a channel.
6. Track notification status and every channel delivery attempt.
7. Retry transient failures with configurable delay and attempt limits.
8. Deduplicate repeated requests and repeated provider callbacks.
9. Query delivery status and an auditable attempt history.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `NotificationRequest` | Validated submission contract |
| `Notification` | Aggregate delivery and recipient state |
| `Recipient` | Addressable user and channel endpoints |
| `Preference` | Consent, quiet hours, and channel ordering |
| `Template` | Versioned content for locale and channel |
| `DeliveryAttempt` | Provider call, status, timestamps, and error |
| `ChannelProvider` | Email, SMS, push, or in-app adapter |
| `RetryPolicy` | Classifies failures and schedules safe retries |

## Business Rules and State Transitions

- Validate required template variables before attempting delivery.
- A request may be `ACCEPTED`, `PROCESSING`, `DELIVERED`, `PARTIALLY_DELIVERED`,
  `FAILED`, or `SUPPRESSED`; define valid transitions.
- Classify failures as transient or permanent before retrying.
- Never retry an opted-out or structurally invalid destination.
- A provider fallback creates another attempt but not another logical notification.
- Preserve the template version used for every attempt.

## Concurrency and Failure Handling

- The same idempotency key must create at most one logical notification.
- Concurrent workers must not deliver the same pending attempt twice.
- Duplicate or out-of-order callbacks must not regress terminal state.
- Partial success across recipients or channels must remain visible.
- Provider exceptions must not prevent other eligible channels from being attempted.

## Demonstration Scenarios

1. Render and deliver a localised email successfully.
2. Suppress a channel because the recipient opted out.
3. Retry a transient provider failure and then succeed.
4. Fall back to a second provider after the first becomes unavailable.
5. Process duplicate requests and callbacks without duplicate logical delivery.
6. Report partial success for a multi-channel notification.

## Extensions

- Scheduled delivery, batching, and digests
- Rate limits per provider or recipient
- Webhook and chat channels
- Template preview and approval workflow

## Related Problems

- [Publish-Subscribe System](../pub_sub_system/README.md)
- [Observer Pattern](../../Patterns/behavioral/observer/README.md)
- [Rate Limiter](../rate_limiter/README.md)

## Reference Solution (Java 17)

**Status:** Implemented and verified. The solution compiles with `--release 17
-Xlint:all`, and its executable demo covers locale fallback, rendering, opt-out
suppression, deterministic retry, provider fallback, request and callback
deduplication, stale-callback protection, and partial multi-channel delivery.

### Architecture

```text
NotificationRequest -- idempotency key --> NotificationService
        |                                      |
        |                                      |-- preferences / quiet hours
        |                                      |-- template resolution + snapshot
        |                                      `-- due-work state machine
        v                                                  |
Recipient + requested channels                             v
                                              ChannelProvider chain
                                              primary -> retry -> fallback
                                                          |
                                                          v
                                      DeliveryAttempt + AuditEvent + status view
```

- `NotificationTemplate` validates required variables and produces an immutable
  `RenderedMessage`. The repository resolves exact locale, then language locale, then
  English, while preserving the chosen template version and locale on every attempt.
- `NotificationPreferences` models explicit consent, ordered channels, and local
  quiet hours, including overnight intervals. Opt-out, quiet time, or a missing
  destination suppresses delivery before any provider call.
- `ChannelProvider` isolates vendor adapters. Failures are explicitly transient or
  permanent; an unexpected adapter exception is contained and classified transient
  so other deliveries continue.
- `RetryPolicy` calculates capped exponential backoff. `processDue(Instant)` executes
  only work due at that instant—there is no sleeping—so tests and schedulers remain
  deterministic.
- A transient failure retries the current provider up to its limit, then moves to the
  next provider in the channel's ordered fallback chain. Permanent failures are not
  retried.
- Each attempt snapshots provider, template version/locale, rendered content,
  timestamps, state, provider message ID, and failure detail. `status` returns those
  attempts together with per-delivery state and a chronological audit trail.
- Synchronized transitions model atomic row claiming in a production store, preventing
  concurrent workers from sending the same due item twice.

### State and Idempotency Rules

| Concern | Rule |
| --- | --- |
| Request replay | Equal request + equal key returns the original notification ID |
| Key conflict | Same key with different input raises `IdempotencyConflictException` |
| Provider result | Synchronous success is `DELIVERED`; async acceptance waits for callback |
| Transient failure | Record attempt, schedule backoff, then retry/fallback |
| Permanent failure | Record attempt and terminate that recipient/channel delivery |
| Callback replay | Duplicate event ID returns `DUPLICATE` without state mutation |
| Stale callback | A callback for a non-latest or terminal attempt cannot regress state |
| Aggregate status | Mixed success and failure/suppression is `PARTIALLY_DELIVERED` |

### Source Layout

```text
src/main/java/com/example/lld/notification_framework/
|-- Channel.java                    |-- Recipient.java
|-- QuietHours.java                 |-- NotificationPreferences.java
|-- NotificationRequest.java        |-- NotificationTemplate.java
|-- RenderedMessage.java            |-- TemplateRepository.java
|-- InMemoryTemplateRepository.java |-- ChannelProvider.java
|-- ProviderMessage.java            |-- ProviderResponse.java
|-- ProviderFailureException.java   |-- FailureType.java
|-- RetryPolicy.java                |-- ProviderCallback.java
|-- NotificationView.java           |-- NotificationException.java
|-- NotificationService.java        |-- ScriptedProvider.java
`-- NotificationDemo.java
```

### Compile and Run

Run from `Problems/notification_framework`:

```bash
mkdir -p /tmp/notification-framework-classes
find src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all \
      -d /tmp/notification-framework-classes
java -cp /tmp/notification-framework-classes \
  com.example.lld.notification_framework.NotificationDemo
```

Expected final line:

```text
Notification Framework demo: all scenarios passed
```

## Python 3 Reference Solution

**Status:** Implemented and verified using only the Python standard library.

- [`python/solution.py`](python/solution.py) intentionally models one logical
  `(recipient, channel)` delivery per request. It keeps immutable request, rendered-template,
  result, and attempt evidence plus narrow preference, template-resolver, and provider
  `Protocol` ports. Fan-out is a coordinator around this core, not part of the live code.
- Submission stores a request fingerprint for replay/conflict semantics and snapshots the
  rendered template before enqueueing. Workers atomically move a due delivery to `SENDING`
  before provider I/O; transient failures schedule bounded injected-clock retry. An
  ambiguous send remains `UNKNOWN`: only provider lookup or a resend with a stable provider
  idempotency token may move it forward. Such resends still respect `max_attempts`, and a
  capability-less provider is never retried blindly.
- [`python/test_solution.py`](python/test_solution.py) has five deterministic tests covering
  render snapshots and suppression, idempotent submission, scheduled retry, resolvable and
  irreducible unknown outcomes, and an eight-worker race that creates one provider attempt.

### One-hour Python coding scope

**Implement live:** request/template/rendered-message dataclasses; `DeliveryStatus`,
immutable attempt records, preference/template/provider ports, and a closed provider-result
taxonomy; idempotent `submit`, one atomic `process_due` claim/transition, injected-clock
retry, and conservative `UNKNOWN` reconciliation. Test suppression without a provider
call, duplicate submission, one scheduled retry, one concurrent claim, and—most
importantly—that `UNKNOWN` is not automatically sent again or retried past the bound.

**Explain rather than type:** HTTP and callback authentication, ORM schemas, transactional
outbox and broker wiring, recipient/channel fan-out and aggregate status, locale-fallback
catalog administration, multi-provider failover, callback deduplication/monotonicity,
distributed leases, real SDK adapters, cryptographic signature verification, cohort
expansion, rate limiting, PII retention, multi-region routing, metrics, and reconciliation
operations.

Run with Python 3.9+ and only the standard library from
`Problems/notification_framework/python`:

```bash
python3 test_solution.py
```
