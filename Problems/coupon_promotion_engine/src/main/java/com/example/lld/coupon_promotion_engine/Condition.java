package com.example.lld.coupon_promotion_engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface Condition {
    Eligibility evaluate(EvaluationContext context);

    default Condition and(Condition other) {
        Objects.requireNonNull(other, "other");
        return context -> {
            Eligibility left = evaluate(context);
            Eligibility right = other.evaluate(context);
            List<String> explanations = new ArrayList<>(left.explanations());
            explanations.addAll(right.explanations());
            return new Eligibility(left.eligible() && right.eligible(), explanations);
        };
    }

    default Condition or(Condition other) {
        Objects.requireNonNull(other, "other");
        return context -> {
            Eligibility left = evaluate(context);
            if (left.eligible()) {
                return left;
            }
            Eligibility right = other.evaluate(context);
            if (right.eligible()) {
                return right;
            }
            List<String> explanations = new ArrayList<>(left.explanations());
            explanations.addAll(right.explanations());
            return new Eligibility(false, explanations);
        };
    }
}
