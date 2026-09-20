# In-Memory Relational Database

## Problem

Design a small in-memory relational database with typed schemas, constraints, indexes, transactions, and a deliberately limited query API.

## Required behavior

- Create and drop tables with named, typed columns and primary-key, uniqueness, and nullability constraints.
- Insert, update, delete, and fetch rows after schema validation.
- Filter, project, sort, limit, and join rows through an expression/query model.
- Maintain hash or ordered indexes consistently with row mutations.
- Execute a transaction atomically with commit and rollback.
- Define isolation for concurrent transactions and detect write conflicts.
- Return explainable constraint and query errors rather than leaking collection failures.

## Core model

`Database`, `Table`, `Schema`, `Column`, `DataType`, `Row`, `Value`, `Constraint`, `Index`, `Transaction`, `Query`, `Expression`, and `QueryPlan`.

Keep parsing optional: the core should accept a typed command/query model. Storage, indexing, planning, and transaction control are separate responsibilities.

## Invariants and edge cases

- Every committed row conforms to the table's current schema and constraints.
- Primary and unique indexes agree with table contents after every commit.
- A failed multi-row statement or transaction exposes no partial mutation.
- Define behavior for null comparison, duplicate column names, stale transactions, schema changes, and index creation on existing data.

## Design exercise

Demonstrate table creation, constrained inserts, indexed lookup, filtered update, join, rollback, two transactions racing on one row, and index consistency after deletion.

## Extensions

SQL parsing, cost-based planning, write-ahead logging, snapshots, foreign keys, and persistence. These are follow-ups, not requirements for the first complete design.
