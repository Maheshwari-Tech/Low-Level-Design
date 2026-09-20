# Airline Management System

## Problem

Design the object model and services for searching flights, operating schedules, and managing a passenger booking from seat selection through travel or cancellation.

## Required behavior

- Search itineraries by origin, destination, travel date, and passenger count.
- Maintain flight schedules, aircraft and seat maps, crew assignments, and flight status.
- Create a temporary seat hold, confirm a booking after payment, and prevent double booking.
- Store passenger and baggage details; support check-in and seat changes.
- Cancel or change a booking and calculate the applicable refund or fare difference.
- Support passenger, staff, and administrator capabilities without mixing their responsibilities.

## Core model

`Airport`, `Flight`, `FlightLeg`, `Schedule`, `Aircraft`, `Seat`, `Passenger`, `Booking`, `Ticket`, `Baggage`, `Payment`, and `CrewAssignment`.

Keep `FlightStatus`, `SeatStatus`, and `BookingStatus` as explicit state machines. Pricing, payment, and notification should be injected policies or ports rather than hard-coded inside `Booking`.

## Invariants and edge cases

- A seat may have at most one active hold or confirmed ticket for a flight.
- A booking is confirmed only when every requested seat is secured and payment succeeds.
- Aircraft changes must remap or invalidate existing seat assignments deliberately.
- Handle hold expiry, duplicate commands, schedule changes, missed check-in, and concurrent booking attempts.

## Design exercise

Expose search, hold, confirm, check-in, change, and cancel use cases. Demonstrate a successful booking, two customers racing for one seat, an expired hold, a cancellation with refund, and an aircraft swap.

## Extensions

Waitlists, multi-leg itineraries, loyalty accounts, overbooking policy, and disruption re-accommodation.
