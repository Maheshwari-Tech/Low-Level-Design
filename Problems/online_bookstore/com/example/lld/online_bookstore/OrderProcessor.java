package com.example.lld.online_bookstore;

import java.util.Map;

public class OrderProcessor {
    private InventoryManager inventoryManager;

    public OrderProcessor(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public boolean placeOrder(Order order) {
        // Check stock for all items
        for (Map.Entry<Book, Integer> entry : order.getItems().entrySet()) {
            Book book = entry.getKey();
            int quantity = entry.getValue();
            if (!inventoryManager.isInStock(book.getIsbn(), quantity)) {
                return false; // Insufficient stock
            }
        }

        // Deduct stock
        for (Map.Entry<Book, Integer> entry : order.getItems().entrySet()) {
            Book book = entry.getKey();
            int quantity = entry.getValue();
            inventoryManager.updateStock(book.getIsbn(), quantity);
        }

        order.setStatus("Paid");
        // In a real system, integrate with payment gateway here

        return true;
    }

    public void fulfillOrder(Order order) {
        order.setStatus("Fulfilled");
        // Handle shipping, etc.
    }
}
