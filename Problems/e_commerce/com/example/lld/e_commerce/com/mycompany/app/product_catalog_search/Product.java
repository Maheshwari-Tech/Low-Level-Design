package com.mycompany.app.product_catalog_search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Product {

    private final int id;
    private final String name;
    private final Category category;
    private double price;
    private final Set<String> tags;
    private static volatile AtomicInteger counter = new AtomicInteger();

    public Product(String name, Category category, double price) {
        this.id = counter.getAndIncrement();
        this.name = name;
        this.category = category;
        this.tags = new HashSet<>();
        this.price = price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void removeTag(String tag){
        this.tags.remove(tag);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static AtomicInteger getCounter() {
        return counter;
    }

    public Category getCategory() {
        return category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public double getPrice() {
        return price;
    }
}
