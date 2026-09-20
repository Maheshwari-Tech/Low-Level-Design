# Splitwise-Like Expense Sharing

## Problem

Design an expense-sharing application that records who paid, divides each expense among participants, maintains pairwise balances, and records settlements for friends and groups.

## Requirements

- Register users and create groups.
- Add an expense with payer, amount, description, category, and participants.
- Support equal, exact-amount, and percentage splits.
- Validate every split before changing balances.
- Show a user's net and user-to-user balances.
- Record partial or full settlements and payment status.
- Keep expense and settlement history.
- Suggest simplified settlements without changing the underlying ledger.

## Core Model

- **`SplitwiseSystem`**: application facade and transaction coordinator.
- **`User`**: identity and profile.
- **`Group`**: membership and group expense history.
- **`Expense`**: payer, participants, total, description, category, and timestamp.
- **`Split`**: a participant's share; specialized as equal, exact, or percentage input.
- **`SplitStrategy`**: validates inputs and produces normalized monetary shares.
- **`BalanceSheet`**: directional user-to-user obligations.
- **`Settlement`**: transfer from one user to another and its status.

## Data Structures and Invariants

- `Map<String, User>` and `Map<String, Group>` for registries.
- `List<Expense>` for an append-only history.
- `Map<UserId, Map<UserId, Money>>` for pairwise balances; store one canonical direction to avoid contradictory entries.
- A net-balance map and priority queues of creditors/debtors for settlement suggestions.

Use a decimal `Money` value rather than binary `double`. Exact shares must sum to the expense amount; percentages must sum to 100; equal splits need a deterministic rounding rule. Adding an expense should append the ledger event and update all derived balances atomically.

## Patterns

- **Strategy** for equal, exact, and percentage calculation.
- **Factory** for validated expense/split construction.
- **Observer** for expense, balance, and settlement notifications.
- A ledger or event model keeps audit history separate from balance projections.

## Expense Flow

1. Resolve the payer, participants, and optional group membership.
2. Validate the amount and requested split.
3. Convert shares to normalized `Money` values whose total exactly matches the expense.
4. Append the expense and update each participant's obligation to the payer.
5. Publish notifications after the transaction commits.

## Implementation Status

This target currently contains the design document only. Split strategies, balance storage, settlements, notifications, persistence, friend connections, categories, and simplification are requirements/design extensions and are not implemented here.
