package com.example.lld.coupon_promotion_engine;

import java.util.Objects;

public record Customer(String id, String segment, String salesChannel) {
    public Customer {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be blank");
        }
        if (Objects.requireNonNull(segment, "segment").isBlank()) {
            throw new IllegalArgumentException("Segment cannot be blank");
        }
        if (Objects.requireNonNull(salesChannel, "salesChannel").isBlank()) {
            throw new IllegalArgumentException("Sales channel cannot be blank");
        }
    }
}
