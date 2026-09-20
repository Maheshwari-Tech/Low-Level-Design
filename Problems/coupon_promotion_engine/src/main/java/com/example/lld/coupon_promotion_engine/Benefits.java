package com.example.lld.coupon_promotion_engine;

import java.util.List;
import java.util.Objects;

public final class Benefits {
    private Benefits() {
    }

    public static Benefit percentage(int basisPoints, Money cap) {
        if (basisPoints <= 0 || basisPoints > 10_000) {
            throw new IllegalArgumentException("Percentage must be in (0, 10000] basis points");
        }
        Objects.requireNonNull(cap, "cap");
        return (context, state) -> {
            Money calculated = state.remainingMerchandise().percentage(basisPoints).min(cap);
            return new BenefitOutcome(
                    state.applyOrderDiscount(calculated),
                    basisPoints / 100.0 + "% off merchandise, capped at " + cap);
        };
    }

    public static Benefit fixed(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Fixed discount must be positive");
        }
        return (context, state) -> new BenefitOutcome(
                state.applyOrderDiscount(amount), amount + " off merchandise");
    }

    public static Benefit buyXGetY(String sku, int buyQuantity, int freeQuantity) {
        Objects.requireNonNull(sku, "sku");
        if (buyQuantity <= 0 || freeQuantity <= 0) {
            throw new IllegalArgumentException("Buy and free quantities must be positive");
        }
        return (context, state) -> {
            CartLine line = context.cart().lines().stream()
                    .filter(candidate -> candidate.sku().equals(sku))
                    .findFirst()
                    .orElse(null);
            if (line == null) {
                return new BenefitOutcome(List.of(), "No qualifying SKU " + sku);
            }
            int groupSize = Math.addExact(buyQuantity, freeQuantity);
            int discountedQuantity = Math.multiplyExact(
                    line.quantity() / groupSize, freeQuantity);
            Money discount = line.unitPrice().multiply(discountedQuantity);
            return new BenefitOutcome(
                    state.applyLineDiscount(sku, discount),
                    "Buy " + buyQuantity + ", get " + freeQuantity
                            + " free on " + sku + " (" + discountedQuantity + " free unit(s))");
        };
    }

    public static Benefit freeShipping(Money cap) {
        Objects.requireNonNull(cap, "cap");
        return (context, state) -> new BenefitOutcome(
                state.applyShippingDiscount(cap), "Free shipping up to " + cap);
    }
}
