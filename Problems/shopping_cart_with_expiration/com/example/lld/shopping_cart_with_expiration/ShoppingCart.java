package com.example.lld.shopping_cart_with_expiration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shopping Cart with TTL Expiration
 *
 * Design Choices:
 * - ConcurrentHashMap for thread-safe operations.
 * - Automatic cleanup on access.
 * - System.currentTimeMillis() for expiration checks.
 */
public class ShoppingCart {
    private final Map<String, CartItem> items;

    public ShoppingCart() {
        this.items = new ConcurrentHashMap<>();
    }

    /**
     * 1. Add items to the cart with TTL
     */
    public synchronized void addItem(String id, String name, double price, long ttlMillis) {
        CartItem item = new CartItem(id, name, price, ttlMillis);
        items.put(id, item);
        cleanup(); // Remove expired items
    }

    /**
     * 2. Automatically remove expired items
     */
    private void cleanup() {
        items.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 3. Retrieve current cart contents (excluding expired)
     */
    public synchronized List<CartItem> getContents() {
        cleanup();
        return new ArrayList<>(items.values());
    }

    /**
     * 4. Calculate total price of valid items
     */
    public synchronized double getTotalPrice() {
        cleanup();
        return items.values().stream()
                .mapToDouble(CartItem::getPrice)
                .sum();
    }

    /**
     * Remove item by ID
     */
    public synchronized boolean removeItem(String id) {
        return items.remove(id) != null;
    }

    /**
     * Get item count
     */
    public synchronized int getItemCount() {
        cleanup();
        return items.size();
    }
}
