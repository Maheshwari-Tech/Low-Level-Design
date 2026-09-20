package com.example.lld.coupon_promotion_engine;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CouponEngineDemo {
    private static final String USD = "USD";
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-12-31T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private CouponEngineDemo() {
    }

    public static void main(String[] args) throws Exception {
        Cart cart = new Cart(
                List.of(
                        new CartLine("CHAIR", "furniture", 3, money(5_000)),
                        new CartLine("LAMP", "lighting", 1, money(5_000))),
                money(1_200));
        Customer customer = new Customer("customer-1", "GOLD", "WEB");

        demonstrateAutomaticAndRejectedCoupon(cart, customer);
        demonstrateExclusivePriority(cart, customer);
        demonstrateCompatibleBenefitsAndBogo(cart, customer);
        demonstrateAtomicFinalUse(cart, customer);

        System.out.println("Coupon / Promotion Engine demo: all scenarios passed");
    }

    private static void demonstrateAutomaticAndRejectedCoupon(Cart cart, Customer customer) {
        Promotion automatic = automatic(
                "AUTO10", 20, false, "order-discount", Conditions.minimumSubtotal(money(10_000)),
                Benefits.percentage(1_000, money(3_000)), UsagePolicy.unlimited());
        Promotion expired = new Promotion(
                "OLD20",
                1,
                Optional.of("OLD20"),
                Promotion.Lifecycle.ACTIVE,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T00:00:00Z"),
                10,
                false,
                Optional.of("order-discount"),
                Conditions.always(),
                Benefits.fixed(money(2_000)),
                UsagePolicy.unlimited());
        PromotionEngine engine = new PromotionEngine(CLOCK, List.of(automatic, expired));
        PromotionEngine.Evaluation preview = engine.preview(
                new EvaluationContext(cart, customer, Set.of("OLD20")));

        check(preview.applied().size() == 1, "automatic percentage should apply");
        check(preview.totalDiscount().equals(money(2_000)), "10% should equal USD 20");
        check(preview.rejected().stream().anyMatch(item -> item.promotionId().equals("OLD20")),
                "expired coupon should explain rejection");
    }

    private static void demonstrateExclusivePriority(Cart cart, Customer customer) {
        Promotion highPriority = automatic(
                "EXCLUSIVE25", 100, true, "exclusive", Conditions.always(),
                Benefits.fixed(money(2_500)), UsagePolicy.unlimited());
        Promotion lowerPriorityButLarger = automatic(
                "EXCLUSIVE40", 90, true, "exclusive", Conditions.always(),
                Benefits.fixed(money(4_000)), UsagePolicy.unlimited());
        PromotionEngine.Evaluation preview = new PromotionEngine(
                CLOCK, List.of(lowerPriorityButLarger, highPriority))
                .preview(new EvaluationContext(cart, customer, Set.of()));

        check(preview.applied().get(0).promotionId().equals("EXCLUSIVE25"),
                "priority must deterministically win exclusive conflict");
        check(preview.applied().size() == 1, "only one exclusive promotion may apply");
    }

    private static void demonstrateCompatibleBenefitsAndBogo(Cart cart, Customer customer) {
        Promotion orderDiscount = automatic(
                "ORDER5", 30, false, "order", Conditions.always(),
                Benefits.fixed(money(500)), UsagePolicy.unlimited());
        Promotion shipping = automatic(
                "SHIPFREE", 20, false, "shipping", Conditions.salesChannel("WEB"),
                Benefits.freeShipping(money(2_000)), UsagePolicy.unlimited());
        Promotion bogo = automatic(
                "CHAIR-BOGO", 10, false, "line:CHAIR",
                Conditions.skuQuantityAtLeast("CHAIR", 3),
                Benefits.buyXGetY("CHAIR", 2, 1),
                UsagePolicy.unlimited());
        PromotionEngine.Evaluation preview = new PromotionEngine(
                CLOCK, List.of(shipping, bogo, orderDiscount))
                .preview(new EvaluationContext(cart, customer, Set.of()));

        check(preview.applied().size() == 3, "compatible order, shipping, and BOGO should stack");
        check(preview.totalDiscount().equals(money(6_700)), "combined discount should be exact");

        Cart insufficientCart = new Cart(
                List.of(new CartLine("CHAIR", "furniture", 2, money(5_000))), money(0));
        PromotionEngine.Evaluation insufficient = new PromotionEngine(CLOCK, List.of(bogo))
                .preview(new EvaluationContext(insufficientCart, customer, Set.of()));
        check(insufficient.applied().isEmpty() && !insufficient.rejected().isEmpty(),
                "insufficient BOGO quantity should be rejected with an explanation");
    }

    private static void demonstrateAtomicFinalUse(Cart cart, Customer customer) throws Exception {
        Promotion lastUse = new Promotion(
                "LAST-USE",
                1,
                Optional.of("LAST"),
                Promotion.Lifecycle.ACTIVE,
                START,
                END,
                1,
                false,
                Optional.of("last-use"),
                Conditions.always(),
                Benefits.fixed(money(1_000)),
                new UsagePolicy(1, 1, true));
        PromotionEngine engine = new PromotionEngine(CLOCK, List.of(lastUse));
        EvaluationContext context = new EvaluationContext(cart, customer, Set.of("LAST"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<PromotionEngine.Redemption>> futures;
        try {
            List<Callable<PromotionEngine.Redemption>> attempts = List.of(
                    () -> engine.redeem("order-A", context),
                    () -> engine.redeem("order-B", context));
            futures = executor.invokeAll(attempts);
        } finally {
            executor.shutdown();
        }

        PromotionEngine.Redemption first = futures.get(0).get();
        PromotionEngine.Redemption second = futures.get(1).get();
        long winners = List.of(first, second).stream()
                .filter(redemption -> !redemption.evaluation().applied().isEmpty())
                .count();
        check(winners == 1L, "only one concurrent redemption may consume the final use");
        PromotionEngine.Redemption winner = first.evaluation().applied().isEmpty() ? second : first;
        PromotionEngine.Redemption retry = engine.redeem(winner.orderId(), context);
        check(retry.equals(winner), "idempotent retry must return the original redemption");
        check(engine.usageSnapshot().globalUses().get("LAST-USE") == 1,
                "retry must not increment usage");

        engine.cancel(winner.orderId());
        check(!engine.usageSnapshot().globalUses().containsKey("LAST-USE"),
                "reversible cancellation must restore quota");
    }

    private static Promotion automatic(
            String id,
            int priority,
            boolean exclusive,
            String stackingGroup,
            Condition condition,
            Benefit benefit,
            UsagePolicy usagePolicy) {
        return new Promotion(
                id,
                1,
                Optional.empty(),
                Promotion.Lifecycle.ACTIVE,
                START,
                END,
                priority,
                exclusive,
                Optional.of(stackingGroup),
                condition,
                benefit,
                usagePolicy);
    }

    private static Money money(long minorUnits) {
        return new Money(USD, minorUnits);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
