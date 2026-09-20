package com.example.lld.coupon_promotion_engine;

import java.util.Objects;

public record DiscountComponent(Scope scope, String reference, Money amount) {
    public enum Scope {
        CART_LINE,
        SHIPPING
    }

    public DiscountComponent {
        Objects.requireNonNull(scope, "scope");
        if (Objects.requireNonNull(reference, "reference").isBlank()) {
            throw new IllegalArgumentException("Discount reference cannot be blank");
        }
        Objects.requireNonNull(amount, "amount");
    }
}
