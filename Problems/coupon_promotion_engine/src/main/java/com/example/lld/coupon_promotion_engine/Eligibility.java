package com.example.lld.coupon_promotion_engine;

import java.util.List;
import java.util.Objects;

public record Eligibility(boolean eligible, List<String> explanations) {
    public Eligibility {
        explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations"));
    }

    public static Eligibility pass() {
        return new Eligibility(true, List.of());
    }

    public static Eligibility fail(String explanation) {
        return new Eligibility(false, List.of(Objects.requireNonNull(explanation, "explanation")));
    }
}
