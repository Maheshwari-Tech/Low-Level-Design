# Shopping Cart with Expiration

## Problem Description

Design a shopping cart system where each item has a time-to-live (TTL) expiration. The system must handle concurrent access safely.

## Features

1. **Add items with TTL**: Associate each item with an expiration time.
2. **Automatic removal**: Expired items are removed automatically.
3. **Retrieve contents**: Get current cart contents excluding expired items.
4. **Calculate total**: Compute price of all valid items.
5. **Thread-safety**: Ensure safe concurrent access.

## Design Decisions

### Architecture
- **TTL-based Expiration**: Each item tracks its own expiration timestamp.
- **Lazy Cleanup**: Expired items removed on access operations.
- **Thread-Safe Operations**: Synchronized methods for cart modifications.

### Key Choices
- **ConcurrentHashMap**: For thread-safe item storage without external locking.
- **System.currentTimeMillis()**: Simple time-based expiration checks.
- **Automatic Cleanup**: Integrated into read operations to maintain consistency.
- **Synchronized Methods**: Ensure atomic operations on cart state.

### Data Structures
- **ConcurrentHashMap<String, CartItem>**: Thread-safe item storage.
- **CartItem**: Encapsulates item data and expiration logic.

### Trade-offs
- **Lazy vs. Eager Cleanup**: Cleanup on access vs. background thread; lazy chosen for simplicity.
- **Time Precision**: Millisecond precision sufficient for cart expiration.
- **Memory vs. Performance**: HashMap provides fast access; cleanup prevents unbounded growth.

## How to Run

1. Compile: `javac com/example/lld/shopping_cart_with_expiration/*.java`
2. Run: `java com.example.lld.shopping_cart_with_expiration.Main`

## Example Output

```
=== Shopping Cart with Expiration ===

Initial cart:
iPhone ($999.99)
Headphones ($199.99)
Charger ($49.99)
Total: $1249.97

After 4 seconds:
iPhone ($999.99)
Headphones ($199.99)
Total: $1199.98

After 7 seconds:
Headphones ($199.99)
Total: $199.99
```

## Extensions

- Add persistence to database
- Implement background cleanup thread
- Add item quantity support
- Include discount calculations
- Add cart session management
