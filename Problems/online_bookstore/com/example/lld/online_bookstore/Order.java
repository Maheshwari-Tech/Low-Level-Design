package com.example.lld.online_bookstore;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;

public class Order {
    private String orderId;
    private Patron patron;
    private Map<Book, Integer> items; // Book and quantity
    private Date orderDate;
    private double totalAmount;
    private String status; // e.g., "Pending", "Paid", "Fulfilled"

    public Order(String orderId, Patron patron) {
        this.orderId = orderId;
        this.patron = patron;
        this.items = new HashMap<>();
        this.orderDate = new Date();
        this.status = "Pending";
        this.totalAmount = 0.0;
    }

    public void addItem(Book book, int quantity) {
        items.put(book, items.getOrDefault(book, 0) + quantity);
        totalAmount += book.getPrice() * quantity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public Patron getPatron() { return patron; }
    public Map<Book, Integer> getItems() { return items; }
    public Date getOrderDate() { return orderDate; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
}
