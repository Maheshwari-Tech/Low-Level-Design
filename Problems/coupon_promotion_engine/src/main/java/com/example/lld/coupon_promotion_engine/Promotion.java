package com.example.lld.coupon_promotion_engine;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record Promotion(
        String id,
        int version,
        Optional<String> couponCode,
        Lifecycle lifecycle,
        Instant activeFrom,
        Instant activeUntil,
        int priority,
        boolean exclusive,
        Optional<String> stackingGroup,
        Condition condition,
        Benefit benefit,
        UsagePolicy usagePolicy) {

    public enum Lifecycle {
        DRAFT,
        ACTIVE,
        PAUSED,
        ENDED
    }

    public Promotion {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("Promotion ID cannot be blank");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("Version must be positive");
        }
        Objects.requireNonNull(couponCode, "couponCode");
        couponCode = couponCode.map(code -> {
            if (code.isBlank()) {
                throw new IllegalArgumentException("Coupon code cannot be blank");
            }
            return code.toUpperCase(Locale.ROOT);
        });
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(activeFrom, "activeFrom");
        Objects.requireNonNull(activeUntil, "activeUntil");
        if (!activeFrom.isBefore(activeUntil)) {
            throw new IllegalArgumentException("Promotion window must be non-empty");
        }
        Objects.requireNonNull(stackingGroup, "stackingGroup");
        stackingGroup = stackingGroup.map(group -> {
            if (group.isBlank()) {
                throw new IllegalArgumentException("Stacking group cannot be blank");
            }
            return group;
        });
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(benefit, "benefit");
        Objects.requireNonNull(usagePolicy, "usagePolicy");
    }
}
