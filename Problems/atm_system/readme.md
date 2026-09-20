# ATM System

## Problem

Design an ATM that authenticates a cardholder, exposes banking operations, dispenses available cash, records transactions, and returns to a safe idle state after every session.

## Requirements

- Insert and eject a card.
- Validate a PIN and limit failed attempts.
- Show balance and recent transaction history.
- Withdraw only when the account has funds and the ATM can compose the requested amount from its cash inventory.
- Update account balance and denomination inventory consistently.
- Change a PIN after appropriate re-authentication.
- Integrate through a banking-service boundary rather than owning bank data.
- Define cancellation, timeout, hardware failure, retained-card, receipt, and audit behavior.

## Core Classes

- **`ATM`**: facade and session context; delegates each operation to its current state.
- **`ATMState`**: contract for card, PIN, transaction, and eject actions.
- **`IdleState`**, **`CardInsertedState`**, **`AuthenticatedState`**: implemented session states.
- **`Card`**: card number and linked account number.
- **`Account`**: balance, PIN, and transaction history.
- **`Transaction`**: immutable type, amount, resulting balance, and timestamp.
- **`BankingService`**: bank integration boundary; `SimpleBankingService` is an in-memory demo.
- **`CashDispenser`**: denomination inventory and greedy cash composition.
- **`Screen`**, **`Keypad`**, and **`ReceiptPrinter`**: useful hardware adapter boundaries, but not present in the current implementation.

## State Flow

```text
Idle --insertCard--> CardInserted --valid PIN--> Authenticated
  ^          |              |                        |
  |          +--eject-------+------------------------+
  +-------------------------eject / session end------+
```

Invalid operations stay in the current state and print guidance. After three failed PIN attempts, the demo ejects and resets the card even though its message says the card was retained; a real retained-card state must make those semantics consistent.

## Data Structures

- `Map<String, Account>` for account lookup in the demo banking service.
- `Map<Integer, Integer>` for denomination-to-note-count inventory.
- `List<Transaction>` for account history.

The account debit and physical dispense should be one compensatable workflow: reserve notes, authorize/debit, dispense, then commit; compensate if hardware fails. The current demo performs these steps synchronously and is not transactional.

## Patterns

- **State** implements session-dependent behavior.
- **Facade** is provided by `ATM` and `BankingService` over lower-level operations.
- **Chain of Responsibility** can model denomination handlers, although the current dispenser uses a loop.
- **Observer** can publish account/audit events, but is not implemented in this demo.

## Implementation Status

Implemented:

- card insertion/ejection and three state objects;
- in-memory PIN authentication with an attempt counter;
- balance display and the five most recent transactions;
- withdrawal against account balance and a denomination inventory;
- in-memory accounts and transaction recording.

Not yet complete:

- `AuthenticatedState.changePin` only prints a placeholder, although `BankingService` exposes a change operation;
- no deposit UI, receipt printer, screen/keypad adapters, timeout, durable audit, encryption, concurrency control, or hardware recovery;
- PINs and sample accounts are demo data, not a secure authentication design;
- amount validation and atomic compensation need strengthening before this is more than an educational simulation.

## Run

Requires Java 17 and Maven:

```bash
cd Problems/atm_system
mvn compile
java -cp target/classes com.example.lld.atm_system.Main
```

The demo prints two test card/PIN pairs at startup and accepts interactive commands.
