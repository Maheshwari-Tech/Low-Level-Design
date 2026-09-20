package com.example.lld.coupon_promotion_engine;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Cart(List<CartLine> lines, Money shipping) {
    public Cart {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one line");
        }
        Objects.requireNonNull(shipping, "shipping");
        Set<String> skus = new HashSet<>();
        for (CartLine line : lines) {
            if (!line.unitPrice().currency().equals(shipping.currency())) {
                throw new IllegalArgumentException("All cart amounts must use one currency");
            }
            if (!skus.add(line.sku())) {
                throw new IllegalArgumentException("Duplicate SKU: " + line.sku());
            }
        }
    }

    public String currency() {
        return shipping.currency();
    }

    public Money merchandiseSubtotal() {
        Money result = Money.zero(currency());
        for (CartLine line : lines) {
            result = result.add(line.subtotal());
        }
        return result;
    }

    public Money total() {
        return merchandiseSubtotal().add(shipping);
    }

    public CartLine line(String sku) {
        return lines.stream()
                .filter(line -> line.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SKU: " + sku));
    }
}
