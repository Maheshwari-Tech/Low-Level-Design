package com.example.lld.vending_machine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Inventory {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public void addProduct(Product product) {
        Product requiredProduct = Objects.requireNonNull(product, "product");
        products.put(requiredProduct.code(), requiredProduct);
    }

    public void restock(String code, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive");
        }
        Product product = requireProduct(code);
        products.put(code, product.withQuantity(product.quantity() + quantity));
    }

    public Product getProduct(String code) {
        return products.get(code);
    }

    public boolean isAvailable(String code) {
        Product product = products.get(code);
        return product != null && product.quantity() > 0;
    }

    public boolean reduceStock(String code) {
        Product product = products.get(code);
        if (product == null || product.quantity() == 0) {
            return false;
        }
        products.put(code, product.withQuantity(product.quantity() - 1));
        return true;
    }

    public Map<String, Product> getAllProducts() {
        return Map.copyOf(products);
    }

    public void displayProducts() {
        System.out.println("Available Products:");
        products.values().stream()
                .filter(product -> product.quantity() > 0)
                .forEach(product -> System.out.printf(
                        "%s: %s - $%.2f (%d left)%n",
                        product.code(),
                        product.name(),
                        product.price(),
                        product.quantity()));
    }

    private Product requireProduct(String code) {
        Product product = products.get(code);
        if (product == null) {
            throw new IllegalArgumentException("Unknown product code: " + code);
        }
        return product;
    }
}
