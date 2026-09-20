# Car Rental System

## Problem

Design a car-rental service in which customers search location-specific inventory, receive a price quote, reserve a vehicle for a date range, pay, collect it, and return it at the same or another location.

## Functional Requirements

- Support vehicle categories such as sedan, SUV, and luxury.
- Search availability by pickup location, date range, and vehicle type.
- Prevent overlapping reservations for the same vehicle.
- Calculate base rental, insurance, taxes, add-ons, and optional one-way fees.
- Maintain customer accounts and booking history.
- Track reservation and vehicle lifecycle states.
- Let administrators add, move, service, and retire fleet vehicles.
- Integrate payment and send booking or cancellation notifications.

## Core Model

- **`CarRentalSystem`**: application facade for search and reservation operations.
- **`Vehicle`**: registration, category, rate, location, and availability/maintenance state.
- **`Location`**: rental station and its fleet.
- **`User`**: customer profile and reservation history.
- **`Reservation`**: pickup/drop-off locations, interval, selected vehicle or category, price, and status.
- **`PricingStrategy`**: computes the quote for a reservation request.
- **`Payment`**: authorization, capture, refund, and payment status.

Key operations are `searchAvailableVehicles(location, interval, type)`, `makeReservation(...)`, `cancelReservation(...)`, `pickUp(...)`, and `returnVehicle(...)`.

## Data and Invariants

- `Map<String, Vehicle>` for inventory by vehicle ID.
- `Map<String, Location>` for station lookup.
- Reservation indexes by vehicle and time interval; a plain `List<Reservation>` is acceptable for an in-memory exercise but requires scanning.
- A `PriorityQueue<Vehicle>` may rank eligible vehicles by distance, rate, or assignment score.
- A vehicle cannot have two active reservations whose half-open intervals overlap.
- Reservation confirmation should occur only after availability is rechecked and payment succeeds; failures must release any temporary hold.

## Patterns

- **Strategy** for pricing and vehicle-assignment policies.
- **Factory** for constructing vehicle subtypes or category-specific objects.
- **Observer** for booking, pickup, return, and cancellation notifications.
- A repository abstraction is a natural boundary when replacing the in-memory maps with persistent storage.

## Typical Flow

1. Validate the requested locations and date range.
2. Find eligible vehicles and return priced options.
3. Place a short-lived hold on the chosen vehicle.
4. Authorize payment and atomically confirm the reservation.
5. Mark the vehicle rented at pickup and available or under inspection at return.

## Implementation Status

This target currently contains the design document only. The classes and APIs above are the intended implementation boundary; inventory persistence, an admin UI, payment-gateway integration, notifications, and concurrent reservation protection are not implemented yet.
