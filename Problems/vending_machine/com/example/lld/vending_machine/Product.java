package com.example.lld.vending_machine;

public record Product(String code, String name, double price, int quantity) {
    public Product {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price < 0 || !Double.isFinite(price)) {
            throw new IllegalArgumentException("Product price must be a finite non-negative value");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative");
        }
    }

    public Product withQuantity(int newQuantity) {
        return new Product(code, name, price, newQuantity);
    }
}
