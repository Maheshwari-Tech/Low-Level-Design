package com.example.lld.shopping_cart_with_expiration;

public class CartItem {
    private String id;
    private String name;
    private double price;
    private long expirationTime;

    public CartItem(String id, String name, double price, long ttlMillis) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.expirationTime = System.currentTimeMillis() + ttlMillis;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public long getExpirationTime() { return expirationTime; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }
}
