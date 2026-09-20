# Online Stock Exchange / Brokerage

## Problem

Design the brokerage-facing order domain for accounts, instruments, holdings, watchlists, deposits/withdrawals, and stock orders. Optionally extend it with an exchange matching engine.

## Requirements

- Register members and maintain account status, cash balance, positions, and watchlists.
- Search instruments and view a market-data snapshot.
- Submit market, limit, and stop-loss orders with quantity and time-in-force.
- Validate account state, buying power, owned quantity, price/tick rules, and duplicate requests.
- Cancel an eligible open order and expose chronological order/trade history.
- Apply fills, including partial fills, atomically to remaining quantity, cash reservations, and positions.
- Deposit or withdraw through pluggable payment methods and publish order notifications.

## Core model

`Stock`, `Member`, `StockPosition`, `StockLot`, `Order`, concrete order types, `OrderPart`, `OrderStatus`, `TimeEnforcementType`, `TransferMoney`, and `Notification`.

Separate brokerage validation/portfolio accounting from an exchange order book. A matching engine should maintain price-time priority with explicit buy and sell books; it should not be hidden inside an account object.

## Invariants and edge cases

- Filled plus remaining quantity equals accepted quantity.
- Reserved cash or shares prevent overspending/double-selling while an order is open.
- Applying a fill and updating positions/cash is one atomic ledger operation.
- Handle partial fill, cancel/fill race, duplicate execution report, market closure, stock split, and rejected transfer.

## Local implementation

The Java tree contains the domain model, order variants, account, inventory/position, payment, search, watchlist, and notification abstractions. It is a design skeleton rather than a verified end-to-end exchange; add repositories, matching/placement orchestration, tests, and a runnable demo to complete it.
