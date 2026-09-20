# Vending Machine

## Problem

Design a vending machine that displays products, accepts a selection and payment, dispenses one item, returns change, handles stock failures, and allows cancellation. The same design can specialize into a configurable coffee machine without creating a separate problem.

## Requirements

- Store product code, name, price, and quantity.
- Display products that are available.
- Reject an unknown or out-of-stock selection.
- Accept positive payments and track the inserted amount.
- Dispense only after sufficient payment, decrement stock once, and return change.
- Cancel from any active transaction and refund the accepted amount.
- Reset selection, payment, and state after success or cancellation.
- Define behavior for hardware failure and out-of-service mode.

If physical coins or notes are modeled, the machine must track denomination counts and return only change it can actually compose. A scalar payment total is sufficient only for a simplified digital-payment demo.

## Core Model

- **`VendingMachine`**: facade and current transaction context.
- **`Product`**: immutable product identity, price, and current quantity snapshot.
- **`Inventory`**: product lookup, availability, restocking, and stock deduction.
- **`PaymentProcessor`**: accepted amount, sufficiency check, change, refund, and reset.
- **`MachineState`**: state-specific selection, payment, dispense, and cancel operations.
- **`IdleState`, `SelectingState`, `PaymentState`**: normal purchase lifecycle; explicit dispensing and out-of-service states can be added when hardware is asynchronous.

## Data Structures and Invariants

- `Map<String, Product>` for inventory by product code.
- An enum or state objects for machine state.
- `Map<Denomination, Integer>` when cash inventory and exact change are required.

A successful transaction must atomically validate stock, debit one item, capture the price, compute/refund change, and reset. A failed dispense must not silently keep money or reduce stock.

## Patterns

- **State** removes large conditionals and defines legal operations in each phase.
- **Strategy** can select cash, card, or wallet payment and different dispensing/preparation mechanisms.
- A single physical machine naturally owns one controller instance, but a process-wide Singleton is unnecessary and makes testing harder; inject the machine where needed.

## Configurable Coffee Specialization

Model coffee as a prepared product rather than a separate system:

- **`Recipe`** maps ingredients such as water, milk, coffee, and sugar to required quantities and a price.
- **`IngredientInventory`** tracks available quantities and supports atomic reserve/consume/release operations.
- **`RecipeCatalog`** lets operators add, update, enable, or disable recipes without code changes.
- **`PreparationStrategy`** performs recipe-specific brewing after payment.

Before accepting final payment, validate that every ingredient is available. Reserve ingredients, prepare the drink, then commit consumption; on failure, release the reservation and refund the customer. Operator actions should include ingredient refill, recipe configuration, cleaning, and maintenance mode.

## Implementation Status

The simplified snack/drink flow is implemented in [`com/example/lld/vending_machine`](com/example/lld/vending_machine). It includes deterministic in-memory inventory, restocking, positive-payment validation, State-based selection/payment/dispense/cancel behavior, stock deduction, refund, change, reset, and a console demo. Cash-denomination inventory, exact-change validation, hardware integration, persistence, an out-of-service state, and configurable coffee recipes/ingredients remain planned extensions.

```bash
javac -d /tmp/lld-build Problems/vending_machine/com/example/lld/vending_machine/*.java
java -cp /tmp/lld-build com.example.lld.vending_machine.Main
```
