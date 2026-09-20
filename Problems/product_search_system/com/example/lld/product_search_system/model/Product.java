package com.example.lld.product_search_system;

import java.util.List;

public class Product {
    private String id;
    private String name;
    private String category;
    private double price;
    private List<String> tags;

    public Product(String id, String name, String category, double price, List<String> tags) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.tags = tags;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public List<String> getTags() { return tags; }

    @Override
    public String toString() {
        return name + " (" + category + ") - $" + price;
    }
}
