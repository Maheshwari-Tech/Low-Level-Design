package com.example.lld.coupon_promotion_engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface Benefit {
    BenefitOutcome apply(EvaluationContext context, PricingState pricingState);

    /** Composes benefits in order; the second benefit sees the first benefit's pricing state. */
    default Benefit and(Benefit other) {
        Objects.requireNonNull(other, "other");
        return (context, pricingState) -> {
            BenefitOutcome first = apply(context, pricingState);
            BenefitOutcome second = other.apply(context, pricingState);
            List<DiscountComponent> components = new ArrayList<>(first.components());
            components.addAll(second.components());
            return new BenefitOutcome(
                    components, first.explanation() + "; " + second.explanation());
        };
    }
}
