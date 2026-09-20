# Problem: Calendar Meet Booking

Build an in-memory calendar meeting scheduler.

## Functional requirements

1. List meetings.
2. Find 30-minute availability for a date and a participant set.
3. Schedule a meeting with one or more participants.
4. Reject an overlapping meeting for any shared participant.

## Scope

- Working hours are 09:00–17:00.
- Meetings use half-open intervals: `[start, end)`. A meeting ending at 10:30 does not conflict with one starting at 10:30.
- Recurrence, rooms, reminders, authentication, and time-zone conversion are out of scope.

## Evaluation examples

- `10:00–10:30` and `10:30–11:00` for the same person are allowed.
- `10:00–10:30` and `10:15–10:45` for the same person conflict.
- Overlapping times for disjoint participant sets are allowed.
