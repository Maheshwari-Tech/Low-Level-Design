# Online Bookstore

## Problem

Design an online bookstore that indexes books, manages stock, associates orders with patrons, and coordinates ordering, payment, and fulfillment.

## Requirements

- Store ISBN, title, author, subject, price, and stock for each book.
- Search by title, author, or subject.
- Add stock, check requested quantities, deduct sold quantities, and restock.
- Maintain patron profiles and order history.
- Create orders with books, quantities, total, timestamp, and status.
- Reject an order when any line is unavailable.
- Separate inventory, payment, and fulfillment concerns.

## Core Classes

- **`Book`**: catalog and stock fields.
- **`Patron`**: customer details and order history.
- **`Order`**: `Map<Book, Integer>` line items, total, date, and status.
- **`SearchService`**: exact, case-insensitive in-memory indexes for title, author, and subject.
- **`InventoryManager`**: ISBN-to-book inventory and stock mutations.
- **`OrderProcessor`**: stock validation, deduction, and order status transitions.

## Data Structures and Complexity

- `Map<String, List<Book>>` for each search index.
- `Map<String, Book>` for inventory by ISBN.
- `Map<Book, Integer>` for order quantities.
- `List<Order>` for a patron's history.

Indexed lookup is average O(1) for an exact normalized key, plus O(k) to return its books. The current indexes do not provide partial-text, typo-tolerant, or ranked search.

## Design Decisions

The implementation favors single-purpose services and composition; it does not currently use an explicit GoF pattern. Natural extensions are Strategy for payment/shipping, State for guarded order transitions, and repositories for persistence.

Order placement should ideally reserve stock, authorize payment, commit stock, and release the reservation on failure. The current in-memory demo only checks and deducts stock before marking the order `Paid`.

## Implementation Status

Implemented in `com/example/lld/online_bookstore/`:

- book, patron, and order models;
- exact-key title/author/subject indexes;
- inventory lookup, availability checks, deduction, and restocking;
- an order-processing demo and a manual fulfillment transition.

Payment and shipping are simulated by status changes; there is no gateway, rollback, concurrency control, persistence, authentication, reviews, or advanced search.

## Run

Run from the repository root:

```bash
javac Problems/online_bookstore/com/example/lld/online_bookstore/*.java
java -cp Problems/online_bookstore com.example.lld.online_bookstore.Main
```
