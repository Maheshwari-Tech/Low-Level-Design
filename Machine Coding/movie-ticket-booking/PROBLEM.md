# Problem: Movie Ticket Booking

Build an in-memory movie ticket booking system.

## Functional requirements

1. List shows.
2. Display the seat map for a show.
3. Hold one or more available seats.
4. Confirm a hold as a booking.
5. Reject double booking and invalid/duplicate seats.

## Scope

- Each show has a fixed seat inventory and price.
- A booking is created only from a valid hold.
- Holds expose an expiry time. Lazy expiration is sufficient for the in-memory solution.
- Payment, authentication, cancellation, and refunds are out of scope.

## Evaluation examples

- Holding `A1, A2` makes them unavailable.
- Another hold for `A1` fails.
- Confirming the original hold creates a booking and permanently books the seats.
- An expired hold cannot be confirmed and its seats become available again.
