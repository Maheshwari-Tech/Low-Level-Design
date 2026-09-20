# Ticket Booking System

## Problem

Design a booking application for movie shows or concerts where many users may compete for the same seats.

## Requirements

- Manage venues, screens or seating sections, events/shows, schedules, and seat maps.
- Search events and retrieve currently available seats.
- Open a booking session and atomically place a temporary hold on a selected group of seats.
- Limit seats per booking and reject any selection containing an unavailable seat.
- Attempt payment with a configurable retry limit.
- Confirm the booking and make seats permanently unavailable after payment succeeds.
- Release every held seat after payment failure, explicit session close, cancellation, or timeout.

## Core model and states

`Theatre`/`Venue`, `Screens`, `Show`, `Movie` or `Event`, `Seat`, `Users`, and `Booking` form the local model. Seat states must distinguish `AVAILABLE`, temporary hold, and booked. A booking/session moves through open, payment pending, confirmed, failed/expired, or cancelled states.

## Concurrency invariants

- Holding a group is all-or-nothing; partial holds are not leaked on failure.
- A seat has at most one active hold/booking for a show.
- Confirmation is valid only for the session that owns the unexpired hold.
- Payment and timeout callbacks may race, so only one terminal transition can win.
- Repeated hold, payment, and callback commands require idempotency.

## Demonstration scenarios

1. Two users view one show; the first user's held seats disappear from the second view and then confirm.
2. Payment fails or the session closes; the seats become available again.
3. Two users select overlapping groups; exactly one atomic hold succeeds.
4. A configurable timeout releases an abandoned hold.

## Local implementation

The Java tree contains venue/show/seat/booking models, controllers, exceptions, and payment coordination. Treat it as implementation material that still needs a complete concurrent hold/session workflow, tests for the four scenarios, and an exact run command.

See [Inventory Reservation](../inventory_reservation_service/), [Payment Processing](../payment_processing_service/), and [Notification Framework](../notification_framework/) for reusable boundaries.
