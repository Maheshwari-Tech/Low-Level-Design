# Restaurant Management System

## Problem

Design the in-restaurant domain for reservations, tables, menus, orders, kitchen work, bills, and payments.

## Required behavior

- Maintain floor sections, tables, capacities, and table availability.
- Create, modify, seat, and cancel reservations without double-booking a table.
- Open a dining session and capture orders with modifiers and price snapshots.
- Route order items to the appropriate kitchen station and track preparation states.
- Amend or void items with authorization and an audit reason.
- Split or merge bills, apply tax/service charge, accept several payment methods, and close the table.
- Track staff assignments and notify servers when items are ready.

## Core model

`Restaurant`, `Table`, `Reservation`, `DiningSession`, `Guest`, `Menu`, `MenuItem`, `Order`, `OrderItem`, `KitchenTicket`, `Bill`, and `Payment`.

Reservation, dining, kitchen, and billing states should be separate; a table being occupied does not imply every order item has the same status.

## Invariants and edge cases

- Confirmed reservations for a table cannot overlap.
- A menu change never rewrites an existing order's price snapshot.
- Kitchen transitions cannot skip required preparation steps.
- A bill closes only when its outstanding amount is zero.
- Handle no-shows, walk-ins, table moves, partial item failure, split tender, and concurrent edits.

## Design exercise

Demonstrate reservation and seating, kitchen routing, an item void, bill split, partial payments, table release, and a conflicting reservation.
