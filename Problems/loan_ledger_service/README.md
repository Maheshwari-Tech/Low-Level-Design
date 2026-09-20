# Loan Ledger / EMI Management Service

## Interview Prompt

Design a ledger that creates a borrower loan, applies scheduled instalments and lump-sum payments, and answers the outstanding balance after a requested instalment number.

## Scope and Requirements

1. Create a loan for a lender, borrower, principal, term, and interest policy.
2. Produce an immutable repayment schedule using exact money arithmetic.
3. Record scheduled payments and borrower prepayments idempotently.
4. Return amount paid, remaining amount, and remaining instalment count at a schedule boundary.
5. Preserve an auditable ledger; corrections are reversing entries, never mutation of posted money.
6. Reject currency mismatches, negative amounts, duplicate commands, and payments after closure.

## Core Model

| Type | Responsibility |
| --- | --- |
| `LoanAccount` | Aggregate root for terms, status, and repayment position |
| `LoanTerms` | Principal, rate, term, currency, and rounding policy |
| `Installment` | Due sequence, due date, principal, interest, and status |
| `LedgerEntry` | Immutable debit/credit fact with idempotency key |
| `PaymentAllocationPolicy` | Applies money to fees, interest, then principal |
| `RepaymentSchedule` | Deterministically generated instalments |
| `LoanRepository` | Versioned account persistence |
| `LedgerRepository` | Append-only entries and balance projection |

## Invariants

- Ledger entries balance and are never edited after posting.
- Outstanding principal and amount due never become negative.
- A payment command is posted at most once.
- The sum of schedule components follows one explicit rounding/remainder policy.
- A loan becomes `CLOSED` only when all payable components reach zero.

## API Sketch

```java
LoanId createLoan(CreateLoan command, String idempotencyKey);
PaymentReceipt postInstallment(LoanId loanId, int installmentNumber, String key);
PaymentReceipt postPrepayment(LoanId loanId, Money amount, String key);
BalanceSnapshot balanceAfter(LoanId loanId, int installmentNumber);
List<LedgerEntry> statement(LoanId loanId, Instant from, Instant to);
```

## Design Solution

Generate the schedule once from immutable `LoanTerms`, including the rule for carrying rounding residue into the final instalment. Serialize commands per loan using an optimistic version or row lock. In the same transaction, append balanced ledger entries, advance instalment state, update the balance projection, and store the idempotency result. Reporting reads the projection but can be rebuilt from entries. A prepayment either shortens the term or recomputes future instalments according to an injected policy.

## Source-Backed Solution Variation

- `References/kumaransg-LLD/ledger_company_navi/` — Java/Maven command-driven ledger with loans, payments, and balance queries. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/ledger_company_navi)

## Interview Follow-Ups

- Late fees, grace periods, and holidays.
- Floating-rate resets and refinancing.
- Chargeback/reversal workflows.
- Double-entry lender accounting.
- Event-sourced rebuilds and statement pagination.

## Implementation Status

This page is the canonical interview design. The actual source-backed command-line variation remains unchanged in the local clone.
