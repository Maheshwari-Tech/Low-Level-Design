package com.example.lld.online_bookstore;

import java.util.List;
import java.util.ArrayList;

public class Patron {
    private String id;
    private String name;
    private String email;
    private List<Order> orders;

    public Patron(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.orders = new ArrayList<>();
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public List<Order> getOrders() { return orders; }
}
