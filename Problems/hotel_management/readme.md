# Hotel Management System

## Problem

Design a hotel-management system for room discovery, reservations, check-in/check-out, guest service, billing, housekeeping, and day-to-day operations.

## Functional Requirements

- Support room types such as standard, deluxe, and suite, with amenities and occupancy limits.
- Search room availability for a date range and room type.
- Create, modify, and cancel reservations without double-booking a room.
- Maintain guest profiles and booking history.
- Check guests in and out and track room state.
- Calculate nightly rates, taxes, add-ons, deposits, and refunds.
- Record payments and an auditable folio.
- Schedule housekeeping and expose operational views for staff.

## Core Model

- **`HotelManagementSystem`**: application facade and workflow coordinator.
- **`Hotel`**: property metadata and room inventory.
- **`Room`**: room number, type, amenities, rate, and operational status.
- **`Guest`**: customer identity, contact details, and stays.
- **`Reservation`**: guest, room/type, stay interval, price, and lifecycle status.
- **`Payment` / `Folio`**: charges, payments, refunds, and balance.
- **`HousekeepingTask`**: room, assignee, priority, and completion status.

## Data and Invariants

- `Map<String, Room>` for room lookup.
- Reservation indexes by room and date interval; a list is adequate only for a small in-memory model.
- A priority queue can rank available rooms or housekeeping tasks.
- Use half-open stays `[checkIn, checkOut)` so a room can be checked out and checked in on the same date.
- A reservation may be confirmed only if its assigned room has no overlapping active stay.
- Room status and reservation status are related but distinct: a vacant room can still be dirty or out of service.

## Patterns

- **Strategy** for seasonal, occupancy-based, corporate, or promotional pricing.
- **State** for reservation and room lifecycles.
- **Observer** for confirmations, reminders, housekeeping events, and operational alerts.
- Repositories isolate the domain from in-memory maps or a database.

## Reservation Flow

1. Validate dates, occupancy, and requested type.
2. Find rooms with no overlapping active reservation.
3. Quote the complete stay price.
4. Hold a room, authorize payment, and confirm atomically.
5. Transition through check-in, occupied, check-out, cleaning, and ready states.

## Implementation Status

This target currently contains the design document only. Search, booking, billing, payments, housekeeping, notifications, persistence, and the admin dashboard are proposed components and are not implemented here.
