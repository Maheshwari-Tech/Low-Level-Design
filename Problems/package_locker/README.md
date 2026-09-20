# Package Locker Service

## Interview brief

Design a package-locker service that assigns one package to one suitable locker compartment, monitors placement and pickup, and guarantees that the compartment can physically hold the package. The source's strict wording says locker size is greater than package size; clarify whether equal dimensions are acceptable and model fit per dimension rather than by one vague size label.

## Scope and variations

- Register locker banks and compartments with dimensions and operational state.
- Reserve the smallest suitable available compartment for an incoming package.
- Support courier placement, recipient notification, authenticated pickup, expiry, and removal.
- Release failed/expired reservations without double-allocating a compartment.
- Keep carrier routing, last-mile optimization, refrigeration, and payment outside the baseline.

## Core model

`LockerBank`, `Compartment`, `Dimensions`, `Package`, `Reservation`, `Placement`, `PickupCredential`, `AccessAttempt`, `Notification`, and `MaintenanceBlock`.

Compartment lifecycle can be `AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE`, with explicit `OUT_OF_SERVICE` transitions. Reservation and placement are different: a reserved compartment is not yet known to contain the package.

## Invariants

- A compartment has at most one active reservation or placed package.
- A package has at most one active compartment assignment.
- Every assigned compartment satisfies the configured fit rule for width, height, depth, weight, and capabilities.
- Placement succeeds only for the reserved package and an unexpired reservation.
- Pickup credential verification and the `OCCUPIED -> AVAILABLE` transition occur atomically from the service's perspective.
- Failed access attempts are rate-limited and audited without storing a plaintext pickup code.
- Maintenance never makes an occupied compartment silently available.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `reserve(package, preferredBanks, ttl, idempotencyKey)` | Atomically select and hold the best-fit compartment. |
| `place(reservationId, courierProof, doorEvidence)` | Confirm physical placement and issue recipient access. |
| `pickup(packageId, credential, idempotencyKey)` | Verify access and complete collection once. |
| `expireReservations(now, cursor)` | Release unused holds safely. |
| `markOutOfService(compartmentId, reason, version)` | Fence a damaged compartment. |
| `availability(bankId, dimensions)` | Return counts without promising a hold. |

## Key flows, concurrency, and failure

Allocation filters operational compartments by capability and dimensions, orders candidates by least wasted capacity, and attempts a conditional `AVAILABLE -> RESERVED` update. If another allocator wins, continue to the next candidate. The reservation response is idempotent by package and request key.

During placement, the service validates courier authorization and reservation expiry, commands the door through a hardware port, then records sensor/scan evidence. Hardware timeout leaves the operation `UNKNOWN`; reconcile door and occupancy state before retrying another assignment.

Pickup uses a short-lived hashed credential or authenticated app token. Concurrent pickup calls use the reservation/package version so only one completes. Expiry is a scheduled conditional transition and cannot release an already occupied compartment.

## Design decisions

- Represent dimensions as value objects and make rotation policy explicit.
- Best-fit reduces fragmentation; first-fit is simpler and may be sufficient at small scale.
- Keep door control and notifications behind ports because physical outcomes can be uncertain.
- Use a durable outbox for placement/pickup notifications.

## Follow-up questions

- Are equal dimensions valid, and may packages rotate?
- How long do reservations and occupied packages remain before escalation?
- Can one bank serve multiple carriers/tenants?
- How are returns, oversized packages, refrigeration, and accessibility handled?
- Which sensor evidence makes door operations trustworthy?

## Provenance and implementation status

- Pinned prompt: [locker fit and one-package-per-locker requirement](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L64-L66).
- Related primer index row: [Amazon Locker Service alternatives](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/solutions.md#L8). It links externally; the primer itself provides no local implementation.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
