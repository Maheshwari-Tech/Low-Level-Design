package com.example.lld.coupon_promotion_engine;

import java.util.Objects;

public final class Conditions {
    private Conditions() {
    }

    public static Condition always() {
        return ignored -> Eligibility.pass();
    }

    public static Condition minimumSubtotal(Money minimum) {
        Objects.requireNonNull(minimum, "minimum");
        return context -> context.cart().merchandiseSubtotal().compareTo(minimum) >= 0
                ? Eligibility.pass()
                : Eligibility.fail("Requires merchandise subtotal of at least " + minimum);
    }

    public static Condition skuQuantityAtLeast(String sku, int minimumQuantity) {
        Objects.requireNonNull(sku, "sku");
        requirePositive(minimumQuantity);
        return context -> {
            int quantity = context.cart().lines().stream()
                    .filter(line -> line.sku().equals(sku))
                    .mapToInt(CartLine::quantity)
                    .sum();
            return quantity >= minimumQuantity
                    ? Eligibility.pass()
                    : Eligibility.fail(
                            "Requires " + minimumQuantity + " unit(s) of SKU " + sku);
        };
    }

    public static Condition categoryQuantityAtLeast(String category, int minimumQuantity) {
        Objects.requireNonNull(category, "category");
        requirePositive(minimumQuantity);
        return context -> {
            int quantity = context.cart().lines().stream()
                    .filter(line -> line.category().equals(category))
                    .mapToInt(CartLine::quantity)
                    .sum();
            return quantity >= minimumQuantity
                    ? Eligibility.pass()
                    : Eligibility.fail(
                            "Requires " + minimumQuantity + " item(s) in category " + category);
        };
    }

    public static Condition customerSegment(String segment) {
        Objects.requireNonNull(segment, "segment");
        return context -> context.customer().segment().equals(segment)
                ? Eligibility.pass()
                : Eligibility.fail("Available only to segment " + segment);
    }

    public static Condition salesChannel(String channel) {
        Objects.requireNonNull(channel, "channel");
        return context -> context.customer().salesChannel().equals(channel)
                ? Eligibility.pass()
                : Eligibility.fail("Available only through channel " + channel);
    }

    private static void requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Minimum quantity must be positive");
        }
    }
}
