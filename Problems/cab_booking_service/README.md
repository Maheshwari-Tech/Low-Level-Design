# Cab Booking / Ride-Sharing Service

## Problem

Design an in-memory service where riders request a non-shared cab and nearby available drivers accept and complete trips.

## Requirements

- Register riders and one-driver/one-cab records.
- Represent locations as Cartesian coordinates and calculate straight-line distance.
- Let drivers update location and switch availability on or off.
- Find an available cab within a configurable pickup radius.
- Create a trip, mark the selected cab unavailable, end the trip, and make the cab available again.
- Return a rider's trip history and fail clearly for unknown riders, cabs, or trips.

## Design

`Rider`, `Cab`, `Location`, and `Trip` form the model. Controllers expose use cases while the in-memory managers own registration and trip state. `TripStatus` makes the lifecycle explicit. Driver selection is the natural extension point for nearest-driver, rating, vehicle-type, or pricing strategies.

## Invariants and concurrency

- A cab can have at most one active trip.
- A trip belongs to one rider and one cab and can end only once.
- Selecting a cab and marking it unavailable must be one atomic decision under concurrent requests.
- Distance ties need a deterministic policy.

## Local implementation

The Java example under [`com/example/lld/cab_booking_service/code`](com/example/lld/cab_booking_service/code/) includes controllers, in-memory managers, models, domain exceptions, and a `Main` demo. It implements one cab type, no ride sharing, Euclidean matching, and no authentication or persistence.

## Extensions

Pluggable matching and fare strategies, driver acceptance, cancellation, live tracking, surge pricing, payment, notifications, ratings, and geospatial indexing.
