# Digital Wallet Service

## Problem

Design a wallet domain that stores balances and moves money safely between users and external funding methods.

## Required behavior

- Register users and create one or more currency-specific wallet accounts.
- Link tokenized bank or card funding methods without storing raw credentials.
- Add funds, withdraw funds, and transfer funds between wallets.
- Record an immutable transaction history and generate account statements.
- Support exact monetary arithmetic, currency validation, fees, and optional conversion.
- Make every money-moving command idempotent and safe under concurrent requests.

## Core model

`Wallet`, `WalletAccount`, `LedgerEntry`, `Transfer`, `Money`, `Currency`, `FundingMethod`, `ExchangeRate`, and `TransactionStatus`.

Use balanced ledger entries as the source of truth; a cached balance must be reconcilable from the ledger. External providers sit behind ports with explicit pending, succeeded, and failed outcomes.

## Invariants and edge cases

- A completed transfer creates equal debit and credit value after declared fees/conversion.
- A wallet cannot spend below its allowed balance.
- Duplicate retries must return the original result rather than move money twice.
- Handle provider timeout, late callback, reversal, self-transfer, unsupported currency, and concurrent withdrawals.

## Design exercise

Demonstrate funding, an internal transfer, insufficient funds, a duplicate request, a pending external withdrawal, reversal, and statement reconciliation.

See [Payment Processing Service](../payment_processing_service/) for merchant payment lifecycles.
