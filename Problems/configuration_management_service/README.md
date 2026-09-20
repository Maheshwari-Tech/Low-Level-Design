# Configuration Management Service

## Interview brief

Design a service that lets users add, delete, search, and subscribe to configuration so updates are delivered to interested clients. Clarify whether values are ordinary configuration, secrets, feature flags, or all three; secrets require a separate storage and disclosure policy.

## Scope and variations

- Store namespaced configuration by tenant, application, environment, and key.
- Create/update/delete with versions and validation.
- Search metadata and authorized non-secret values.
- Subscribe by exact key or prefix and resume from a revision cursor.
- Fetch consistent snapshots for startup and incremental changes afterward.
- Support staged rollout or approval as follow-ups, not hidden baseline behavior.

## Core model

`Namespace`, `ConfigKey`, `ConfigValue`, `ConfigVersion`, `Schema`, `ChangeSet`, `Revision`, `Subscription`, `ChangeEvent`, `Snapshot`, `AccessPolicy`, and `SecretReference`.

The ordered change log and current materialized value are one logical commit. Delete creates a versioned tombstone so offline subscribers can learn that a key disappeared.

## Invariants

- Each successful mutation advances the namespace revision exactly once.
- Compare-and-set rejects stale writers rather than silently overwriting a newer value.
- A snapshot is internally consistent at one revision.
- Subscribers receive changes after their cursor in revision order; duplicate delivery is allowed and identifiable.
- A delete tombstone remains until every supported replay window no longer needs it.
- Unauthorized or secret values never appear in search, events, logs, or error messages.
- A value satisfies its declared schema before it becomes current.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `put(scope, key, value, expectedVersion, idempotencyKey)` | Validate and create/update one key. |
| `delete(scope, key, expectedVersion, idempotencyKey)` | Commit a tombstone. |
| `get(scope, key, revision?)` | Read an authorized current/snapshot value. |
| `search(scope, prefixOrMetadata, cursor)` | Find accessible configuration. |
| `snapshot(scope, prefix)` | Return values plus a common revision cursor. |
| `subscribe(scope, prefix, afterRevision)` | Stream ordered changes and heartbeats. |
| `history(scope, key, cursor)` | Audit versions without leaking protected values. |

## Key flows, concurrency, and failure

A write authenticates the actor, validates schema and expected version, appends the revision/change event, updates current state, and commits audit evidence atomically. Notification delivery occurs from the durable log, not directly inside the write request.

A client obtains `snapshot(...)->revision R`, applies the values, then subscribes after `R`; this closes the snapshot/subscribe race. Events are at-least-once, so clients ignore revisions they already applied. If a cursor is older than retention, the server requires a fresh snapshot.

Competing updates use optimistic versions. Cache invalidation includes the namespace revision. A regional partition either rejects writes or has an explicit ownership/conflict policy; do not casually promise multi-master ordering.

## Design decisions

- Prefer immutable versions and a durable change log over in-place values plus best-effort callbacks.
- Keep ordinary values and secret material separate; configuration can contain secret references.
- Scope keys structurally rather than embedding tenant/environment into arbitrary strings.
- Make subscription delivery resumable with cursors instead of claiming exactly once.

## Follow-up questions

- What value size, write rate, subscriber count, history, and propagation latency apply?
- Is consistency global, regional, or per namespace?
- Are schemas, approvals, staged rollout, feature-flag evaluation, or rollback required?
- Which clients may search values versus metadata only?
- How are secrets encrypted, rotated, leased, and audited?

## Provenance and implementation status

- Pinned prompt: [add/delete/search/subscribe configuration](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L99-L103).
- Related primer index row: [Configuration Management System](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/solutions.md#L22). It links externally; the primer itself provides no local implementation.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
