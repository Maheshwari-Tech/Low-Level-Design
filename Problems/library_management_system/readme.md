# Library Management System

## Problem

Design a library system that manages books and members, lends and returns copies, tracks due dates, calculates overdue fines, and protects lending rules.

## Requirements

- Add books with ISBN, title, author, total copies, and available copies.
- Register members.
- Lend an available copy for a configured loan period.
- Return a copy and close its loan.
- Detect overdue loans and calculate a per-day fine.
- Prevent a member with an overdue loan from borrowing another book.
- Keep active-loan and member-loan views consistent.

## Core Model

- **`Book`**: bibliographic identity and copy counts; lends and returns one copy without violating `0 <= available <= total`.
- **`Member`**: profile and current loans; answers whether any loan is overdue.
- **`Loan`**: issue, due, and return dates; owns overdue and fine calculations.
- **`Library`**: central application service for book/member registration, lending, and returns.

## Data Structures

- `Map<String, Book>` keyed by ISBN.
- `Map<String, Member>` keyed by member ID.
- `Map<String, Loan>` keyed by loan ID for active loans.
- A member-local `List<Loan>` for the member's current loans.

These maps provide average O(1) entity lookup. Lending and returning must update the book, active-loan map, and member loan collection as one logical transaction.

## Design Notes

The reference design relies on encapsulation and single-purpose classes rather than forcing a GoF pattern. Useful extension points include repositories for persistence, a fine policy strategy, and observers for due-date reminders.

The main flow is:

1. Resolve member and book.
2. Reject missing entities, unavailable copies, or a member with overdue loans.
3. Decrement availability and create an active loan with a due date.
4. On return, record the return, restore availability, and remove the active/member loan.

## Implementation Status

The lending baseline is implemented in [`com/example/lld/library_management_system`](com/example/lld/library_management_system). It validates book/member registration, maintains copy counts and active/member loan views, uses collision-free sequence IDs, enforces positive loan periods, prevents lending to members with overdue loans, calculates fines, and includes a console demo. Notifications, advanced search, reservations/holds, persistent loan history, configurable fine policies, and payment collection remain extensions.

```bash
javac -d /tmp/lld-build Problems/library_management_system/com/example/lld/library_management_system/*.java
java -cp /tmp/lld-build com.example.lld.library_management_system.Main
```
