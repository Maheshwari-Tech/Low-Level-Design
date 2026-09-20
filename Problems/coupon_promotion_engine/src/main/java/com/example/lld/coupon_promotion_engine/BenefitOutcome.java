package com.example.lld.coupon_promotion_engine;

import java.util.List;
import java.util.Objects;

public record BenefitOutcome(List<DiscountComponent> components, String explanation) {
    public BenefitOutcome {
        components = List.copyOf(Objects.requireNonNull(components, "components"));
        if (Objects.requireNonNull(explanation, "explanation").isBlank()) {
            throw new IllegalArgumentException("Benefit explanation cannot be blank");
        }
    }

    public Money total(String currency) {
        Money total = Money.zero(currency);
        for (DiscountComponent component : components) {
            total = total.add(component.amount());
        }
        return total;
    }
}
