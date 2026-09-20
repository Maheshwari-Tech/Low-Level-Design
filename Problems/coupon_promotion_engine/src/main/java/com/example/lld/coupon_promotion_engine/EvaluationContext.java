package com.example.lld.coupon_promotion_engine;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record EvaluationContext(Cart cart, Customer customer, Set<String> enteredCouponCodes) {
    public EvaluationContext {
        Objects.requireNonNull(cart, "cart");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(enteredCouponCodes, "enteredCouponCodes");
        TreeSet<String> normalizedCodes = new TreeSet<>();
        for (String code : enteredCouponCodes) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Coupon code cannot be blank");
            }
            normalizedCodes.add(code.toUpperCase(Locale.ROOT));
        }
        enteredCouponCodes = Set.copyOf(normalizedCodes);
    }
}
