# Food Delivery Service

## Problem

Design the core domain of a food-delivery marketplace from restaurant discovery through order delivery.

## Required behavior

- Manage customers, restaurants, service areas, menus, item availability, and opening hours.
- Build a cart from one restaurant and preserve item, modifier, tax, and price snapshots.
- Place an order only after restaurant validation, payment, and inventory checks.
- Let a restaurant accept, reject, prepare, and mark an order ready.
- Assign an eligible delivery partner and track pickup and delivery states.
- Calculate item totals, fees, tax, discount, and tip as explicit line items.
- Notify participants of state changes through an injected interface.

## Core model

`Restaurant`, `Menu`, `MenuItem`, `Cart`, `Order`, `OrderLine`, `Customer`, `Courier`, `Delivery`, `Location`, `Money`, and `OrderStatus`.

Keep ordering, dispatch, payment, and notification behind clear boundaries. Matching and fee calculations should be replaceable strategies.

## Invariants and edge cases

- A cart cannot mix restaurants.
- Order state transitions are monotonic and role-authorized.
- One courier cannot accept conflicting active assignments.
- Handle price changes, restaurant timeout, rejected payment, cancellation after preparation, unavailable courier, and duplicate callbacks.

## Design exercise

Demonstrate a successful order, unavailable item, restaurant rejection, courier reassignment, customer cancellation, and delivery completion.
