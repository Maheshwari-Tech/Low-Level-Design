# Distributed ID Generator

## Interview brief

Design a unique-ID service that starts on one node and scales without ever issuing the same ID twice. Preserve both source variations:

1. millions of requests beginning with one server; and
2. a UUID generator framed with extreme population, lifetime, device, and request-rate assumptions.

The solution-index row additionally points to a Twitter Snowflake-style design. Treat Snowflake as one candidate, not as an unstated requirement.

## Scope and variations

- Generate IDs without a database round trip on every request.
- Define whether IDs must be numeric, fixed-width, opaque, sortable, or unpredictable.
- Scale node allocation independently from request generation.
- Decode operational fields only when the chosen format intentionally exposes them.
- Preserve a UUID/random alternative when predictability or offline generation matters more than ordering.

## Core model

`IdGenerator`, `IdLayout`, `NodeIdentity`, `NodeLease`, `TimeSource`, `Sequence`, `Epoch`, `ClockRegressionPolicy`, and `IdDecoder`.

For a Snowflake-like layout, an ID combines elapsed time, a leased node identifier, and a per-time-unit sequence. A UUID-style generator instead relies on sufficient random/namespace entropy and does not need a node lease.

## Invariants

- No two successful calls may return the same ID within the system's stated lifetime.
- A node identifier cannot be used by two live generators during overlapping lease epochs.
- The sequence never wraps inside the same timestamp bucket; generation waits or advances the logical clock first.
- Clock regression cannot reuse a previously emitted `(time, node, sequence)` tuple.
- An ID reported as successful is never recycled after restart or partial failure.
- Sortability, if promised, is precisely scoped: usually per generator or approximately across nodes, not universal event order.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `nextId(namespace)` | Return one ID or an explicit capacity/clock error. |
| `nextBatch(namespace, count)` | Reserve an efficient bounded batch without overlap. |
| `leaseNode(generatorIdentity, ttl)` | Assign a unique node epoch to one generator. |
| `renewNodeLease(leaseId, version)` | Prevent split-brain node reuse. |
| `decode(id)` | Return documented fields when the layout is intentionally decodable. |
| `capacity()` | Report bit allocation, rollover date, and maximum rate. |

## Key flows, concurrency, and failure

On a node, serialize only the timestamp/sequence update with an atomic state word or a small lock. If the sequence is exhausted, wait for the next monotonic bucket with a bounded timeout. Persist or lease a node epoch before serving traffic.

If wall time moves backwards, fail closed, wait within a small tolerance, or use a persisted logical timestamp; never silently reset the sequence. If the lease store becomes unavailable, an existing lease may continue only until its safe expiry. A restarted process must not assume its previous node identity without fencing.

Capacity math belongs in the interview: derive time bits from lifetime and resolution, node bits from maximum simultaneous generators, and sequence bits from peak IDs per time unit. Check that the resulting rollover date and throughput satisfy both prompt variations.

## Design decisions

- Snowflake-like IDs suit compact, roughly time-ordered numeric identifiers.
- UUIDv4/UUIDv7-style IDs suit decentralized generation; UUIDv7 adds time ordering without a central node allocator.
- Do not expose business meaning or personal data in the ID.
- A range allocator is simpler for database-backed systems but creates gaps and needs careful lease fencing.

## Follow-up questions

- Must IDs be unguessable, sortable, numeric, or generated offline?
- What is the peak rate per node and maximum simultaneous node count?
- Is a small probability of collision acceptable, or is deterministic uniqueness required?
- What happens when clocks regress, leases expire, or a region is partitioned?
- How is rollover migrated before the timestamp field is exhausted?

## Provenance and implementation status

- Pinned prompts: [single-node-to-millions unique IDs](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L176) and [planet-scale UUID sizing assumptions](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L284-L290).
- Related primer index row: [Distributed ID Generation / Twitter Snowflake](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/solutions.md#L10). It links externally; the primer itself provides no local implementation.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
