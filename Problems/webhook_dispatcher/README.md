# Webhook Dispatcher

## Interview brief

Design a service that reliably delivers domain events to customer-owned HTTP endpoints. The source prompt is intentionally terse, so state the delivery contract before drawing classes: at-least-once is the practical default, while ordering, batching, and exactly-once processing require explicit consumer cooperation.

## Scope and variations

- Register, verify, rotate, pause, and delete webhook subscriptions.
- Accept immutable events after the producer's business transaction commits.
- Filter subscriptions by event type and optional tenant-safe predicates.
- Sign requests, retry transient failures, rate-limit destinations, and expose delivery history.
- Support replay by an operator without changing the original event identity.
- Treat batch delivery, per-key ordering, and multi-region dispatch as follow-up variations rather than hidden baseline promises.

## Core model

`WebhookEndpoint`, `Subscription`, `EventEnvelope`, `Delivery`, `DeliveryAttempt`, `RetryPolicy`, `SigningSecret`, `DestinationPolicy`, `DeadLetterRecord`, and `DispatchLease`.

An event and a delivery are different aggregates: one event can create many destination-specific deliveries, each with independent attempts and terminal state.

## Invariants

- An accepted event has a stable event ID, type, tenant, timestamp, schema version, and immutable payload.
- At most one logical delivery exists for an `(eventId, subscriptionId)` pair.
- Attempt numbers increase monotonically and only one worker owns an unexpired delivery lease.
- A successful 2xx acknowledgement is terminal unless an operator creates an explicit replay.
- Every request signature covers the raw body, event ID, destination, and timestamp.
- Paused or deleted subscriptions receive no newly scheduled deliveries.
- Retries are bounded; terminal failures remain inspectable and replayable.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `createSubscription(tenant, endpoint, eventTypes, secretPolicy)` | Register a destination and begin verification. |
| `publish(event, idempotencyKey)` | Persist one immutable event and schedule matching deliveries. |
| `listDeliveries(subscriptionId, cursor, status)` | Inspect delivery state and attempts. |
| `replay(deliveryId, operator, reason)` | Create an auditable replay using the original event. |
| `rotateSecret(subscriptionId)` | Overlap old/new verification windows safely. |
| `pauseSubscription(subscriptionId, version)` | Stop future scheduling with optimistic concurrency. |

## Key flows, concurrency, and failure

Use a transactional outbox when accepting events from another service. A scheduler materializes deliveries using a uniqueness constraint, and workers claim due deliveries with a lease. The worker sends the exact serialized body, records status/latency, and either marks success or computes exponential backoff with jitter.

Two workers racing for a delivery are resolved by lease version or compare-and-set. A worker crash after the remote endpoint processed the request but before local acknowledgement causes a retry; therefore receivers deduplicate on event ID. Optional ordering is enforced only within a declared ordering key and may reduce throughput.

Classify failures: retry timeouts, connection errors, 408, 429, and selected 5xx responses; usually stop on permanent 4xx responses. Honor a bounded `Retry-After`. Protect the dispatcher from SSRF, private-network targets, redirect escapes, slow reads, oversized responses, and destinations that continually fail.

## Design decisions

- Promise at-least-once delivery and make event IDs prominent instead of claiming network-level exactly once.
- Separate scheduling from HTTP delivery so backoff and rate limits do not block event ingestion.
- Maintain destination-level concurrency limits and circuit breakers to isolate noisy subscribers.
- Store payload schema versions and preserve the raw signed bytes for deterministic retries.

## Follow-up questions

- What event volume, payload size, retention, and end-to-end latency are required?
- Is ordering global, per subscription, or per business aggregate?
- Which HTTP statuses are retryable, and how long is replay available?
- Must payloads be encrypted per tenant in addition to TLS and signatures?
- How are secret rotation, endpoint ownership verification, and regional residency handled?

## Provenance and implementation status

- Pinned prompt: [Design Webhook Dispatcher](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L241).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
