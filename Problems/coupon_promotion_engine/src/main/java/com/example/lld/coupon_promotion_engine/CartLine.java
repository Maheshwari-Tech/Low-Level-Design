package com.example.lld.coupon_promotion_engine;

import java.util.Objects;

public record CartLine(String sku, String category, int quantity, Money unitPrice) {
    public CartLine {
        if (Objects.requireNonNull(sku, "sku").isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (Objects.requireNonNull(category, "category").isBlank()) {
            throw new IllegalArgumentException("Category cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
