package com.example.lld.coupon_promotion_engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Mutable state scoped to one evaluation; order discounts allocate in cart-line order. */
public final class PricingState {
    private final String currency;
    private final LinkedHashMap<String, Money> remainingBySku = new LinkedHashMap<>();
    private Money remainingShipping;

    public PricingState(Cart cart) {
        Objects.requireNonNull(cart, "cart");
        currency = cart.currency();
        for (CartLine line : cart.lines()) {
            remainingBySku.put(line.sku(), line.subtotal());
        }
        remainingShipping = cart.shipping();
    }

    public Money remainingMerchandise() {
        Money total = Money.zero(currency);
        for (Money amount : remainingBySku.values()) {
            total = total.add(amount);
        }
        return total;
    }

    public Money remainingShipping() {
        return remainingShipping;
    }

    public Money remainingTotal() {
        return remainingMerchandise().add(remainingShipping);
    }

    public List<DiscountComponent> applyOrderDiscount(Money requested) {
        Money amount = requested.min(remainingMerchandise());
        long unallocated = amount.minorUnits();
        List<DiscountComponent> components = new ArrayList<>();
        for (Map.Entry<String, Money> entry : remainingBySku.entrySet()) {
            if (unallocated == 0L) {
                break;
            }
            Money remaining = entry.getValue();
            long allocatedMinor = Math.min(remaining.minorUnits(), unallocated);
            Money allocated = new Money(currency, allocatedMinor);
            entry.setValue(remaining.subtract(allocated));
            components.add(new DiscountComponent(
                    DiscountComponent.Scope.CART_LINE, entry.getKey(), allocated));
            unallocated -= allocatedMinor;
        }
        return List.copyOf(components);
    }

    public List<DiscountComponent> applyLineDiscount(String sku, Money requested) {
        Money remaining = remainingBySku.get(sku);
        if (remaining == null) {
            return List.of();
        }
        Money amount = requested.min(remaining);
        if (amount.isZero()) {
            return List.of();
        }
        remainingBySku.put(sku, remaining.subtract(amount));
        return List.of(new DiscountComponent(
                DiscountComponent.Scope.CART_LINE, sku, amount));
    }

    public List<DiscountComponent> applyShippingDiscount(Money requested) {
        Money amount = requested.min(remainingShipping);
        if (amount.isZero()) {
            return List.of();
        }
        remainingShipping = remainingShipping.subtract(amount);
        return List.of(new DiscountComponent(
                DiscountComponent.Scope.SHIPPING, "shipping", amount));
    }
}
