package com.example.lld.coupon_promotion_engine;

/** Zero means unlimited for the corresponding limit. */
public record UsagePolicy(int globalLimit, int perCustomerLimit, boolean reversible) {
    public UsagePolicy {
        if (globalLimit < 0 || perCustomerLimit < 0) {
            throw new IllegalArgumentException("Usage limits cannot be negative");
        }
    }

    public static UsagePolicy unlimited() {
        return new UsagePolicy(0, 0, true);
    }
}
