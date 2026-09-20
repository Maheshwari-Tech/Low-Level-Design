# Event Calendar / Meeting Scheduler

## Interview Prompt

Design a calendar that lets users and teams create events, invite participants, inspect availability, and optionally reserve a meeting room. The same canonical question covers three distinct implementations in the pinned `kumaransg/LLD` clone; their different scopes are preserved below.

## Scope and Requirements

1. Create, reschedule, and cancel an event with a start, end, organizer, and participants.
2. Query a user's or team's events over a time range.
3. Find a common free slot for required participants.
4. Track invitation responses without overwriting a newer event revision.
5. Optionally allocate a room that has enough capacity and is free for the interval.
6. Notify participants after a committed change; notification failure must not roll back the calendar.

Recurrence expansion, cross-time-zone display, and external calendar synchronization are useful follow-ups, not required for the first implementation.

## Core Model

| Type | Responsibility |
| --- | --- |
| `Calendar` | Owns event references for one user or team |
| `Event` | Aggregate root for interval, organizer, participants, room, and lifecycle |
| `Participant` | User plus required/optional role and response |
| `TimeRange` | Validated half-open interval `[start, end)` |
| `MeetingRoom` | Capacity and reservable-resource identity |
| `AvailabilityService` | Computes busy intervals and candidate free slots |
| `EventRepository` | Atomic event/version persistence |
| `NotificationPort` | Sends committed invitation and update messages |

## Invariants

- `start < end`; adjacent half-open intervals do not overlap.
- An organizer is always a participant and cannot decline their own event.
- One room cannot be reserved by overlapping live events.
- A stale `expectedVersion` cannot overwrite a newer reschedule or cancellation.
- A retried create command with the same idempotency key returns the original event.

## API Sketch

```java
Event create(CreateEventCommand command, String idempotencyKey);
Event reschedule(EventId id, long expectedVersion, TimeRange next);
Event respond(EventId id, long expectedVersion, UserId user, Response response);
void cancel(EventId id, long expectedVersion, UserId actor);
List<Event> events(CalendarId calendar, TimeRange range);
List<TimeRange> freeSlots(Set<UserId> users, TimeRange searchRange, Duration length);
```

## Flow, Concurrency, and Failure

Normalize busy intervals, merge overlaps per participant, then intersect their free intervals. On create or reschedule, lock or transactionally compare the affected event and room-reservation rows. Commit the event and an outbox notification together; deliver email or push asynchronously. If two commands claim the same room interval, a uniqueness/overlap constraint lets exactly one commit. Treat notification retries as idempotent by `(eventId, version, recipient)`.

## Source-Backed Solution Variations

The upstream clone is unchanged; these are real local paths, not recreated code.

1. `References/kumaransg-LLD/Event_calendar_flipkart/` — Java/IntelliJ plus the original PDF; users, teams, participants, slots, and events. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Event_calendar_flipkart)
2. `References/kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designMeetingScheduler/` — room-aware meeting scheduler with attendees and email. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Low-Level-Design-1/designMeetingScheduler)
3. `References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/meetingscheduler/` — a smaller alternate model inside a Maven collection. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/meetingscheduler)

## Interview Follow-Ups

- Recurring-series edits: this occurrence, this and following, or entire series.
- Time-zone and daylight-saving boundaries.
- Privacy-preserving free/busy results.
- Tentative room holds and automatic room replacement.
- CalDAV/Google Calendar synchronization and conflict resolution.

## Implementation Status

This canonical page is an interview-ready design solution. The three source variations remain runnable or inspectable in the exact local clone; they are not copied here because that repository has no repository-wide license.
