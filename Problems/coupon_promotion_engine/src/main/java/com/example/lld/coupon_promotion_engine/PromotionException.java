package com.example.lld.coupon_promotion_engine;

/** Base type for failures raised by the promotion domain. */
public class PromotionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PromotionException(String message) {
        super(message);
    }

    /** Raised when an idempotency key is reused for a different operation. */
    public static final class IdempotencyConflictException extends PromotionException {
        private static final long serialVersionUID = 1L;

        public IdempotencyConflictException(String message) {
            super(message);
        }
    }

    /** Raised when a consumed promotion cannot be rolled back. */
    public static final class RedemptionNotReversibleException extends PromotionException {
        private static final long serialVersionUID = 1L;

        public RedemptionNotReversibleException(String message) {
            super(message);
        }
    }
}
