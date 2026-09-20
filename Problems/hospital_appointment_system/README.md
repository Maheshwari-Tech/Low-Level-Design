# Hospital Appointment System

## Interview brief

Design appointment booking in which every doctor can independently open slots for arbitrary periods. The key modeling question is whether doctors publish discrete slots, recurring availability windows, or both; avoid hard-coding a hospital-wide timetable.

## Scope and variations

- Register doctors, specialties, locations, visit modes, and scheduling policies.
- Let each doctor publish one-off or recurring availability and block time independently.
- Search availability and place a short hold before confirmation.
- Book, reschedule, cancel, check in, complete, or mark no-show.
- Support patient and staff actors with auditable overrides.
- Keep medical records, diagnosis, billing/insurance adjudication, and emergency triage outside the baseline.

## Core model

`Doctor`, `Patient`, `CareLocation`, `VisitType`, `AvailabilityRule`, `AvailabilityException`, `Slot`, `AppointmentHold`, `Appointment`, `SchedulingPolicy`, `WaitlistEntry`, and `Notification`.

Availability rules generate candidate slots in a time range; committed appointments and exceptions determine actual bookability. Store instants plus the originating time zone for display and recurring-rule evaluation.

## Invariants

- A doctor cannot have overlapping confirmed appointments unless the visit type explicitly permits capacity greater than one.
- A confirmed appointment consumes exactly one eligible slot/capacity unit.
- A hold expires once and cannot be confirmed after cancellation or expiry.
- Availability derived from a doctor's rule never overrides a block, leave, or existing appointment.
- Rescheduling atomically acquires the new slot before releasing or superseding the old appointment.
- Patient and doctor identities, access, and audit data remain tenant/facility scoped.
- Reminder failure does not cancel an otherwise valid appointment.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `setAvailability(doctorId, ruleOrWindow, expectedVersion)` | Publish independent recurring or one-off availability. |
| `blockTime(doctorId, interval, reason, expectedVersion)` | Add leave or an exception. |
| `searchAvailability(criteria, range, cursor)` | Return currently bookable candidates. |
| `holdSlot(slotRef, patientId, ttl, idempotencyKey)` | Conditionally reserve capacity. |
| `confirmAppointment(holdId, details, idempotencyKey)` | Create one appointment from a valid hold. |
| `reschedule(appointmentId, newSlotRef, expectedVersion)` | Move without double booking. |
| `cancel(appointmentId, actor, reason, expectedVersion)` | Release capacity and trigger waitlist policy. |

## Key flows, concurrency, and failure

Search expands doctor-specific rules over the requested time range, subtracts exceptions and committed occupancy, and returns candidates—not guarantees. Booking conditionally inserts a hold under a uniqueness/capacity constraint. Concurrent patients can view the same candidate, but only the allowed number of holds/appointments wins.

Confirmation verifies hold ownership and expiry, creates the appointment, consumes the hold, and emits reminders through an outbox. Retries use the same idempotency key. A payment/insurance precondition, if added, needs explicit pending and compensation states rather than holding a slot indefinitely.

Recurring-rule edits are versioned: clarify whether they affect only future unbooked slots or require migration/conflict reports for existing appointments. Handle daylight-saving gaps/overlaps using the doctor's location time zone.

## Design decisions

- Store availability rules plus exceptions instead of materializing infinite future slots.
- Materialize a bounded booking horizon for efficient search and refresh it idempotently.
- Enforce overlap/capacity at the authoritative store, not only in application memory.
- Treat notifications and waitlist offers as asynchronous side effects.

## Follow-up questions

- What slot duration, cleanup buffer, overbooking, and group-session rules apply?
- Can appointments span locations, equipment, or multiple clinicians?
- How far ahead may each doctor open availability?
- How do waitlists, telehealth, referrals, insurance, and deposits change confirmation?
- What privacy, audit, and regional health-data requirements apply?

## Provenance and implementation status

- Pinned prompt: [hospital appointment booking with independently opened doctor slots](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L198).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
