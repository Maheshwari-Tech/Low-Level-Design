package com.example.lld.coupon_promotion_engine;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Evaluates immutable promotion definitions and owns an in-memory, atomic redemption ledger.
 * In production, the synchronized ledger boundary maps to one serializable database transaction.
 */
public final class PromotionEngine {
    public record DiscountLine(
            String promotionId,
            int promotionVersion,
            Optional<String> couponCode,
            String explanation,
            List<DiscountComponent> components,
            Money total) {
        public DiscountLine {
            Objects.requireNonNull(promotionId, "promotionId");
            Objects.requireNonNull(couponCode, "couponCode");
            Objects.requireNonNull(explanation, "explanation");
            components = List.copyOf(Objects.requireNonNull(components, "components"));
            Objects.requireNonNull(total, "total");
        }
    }

    public record RejectedPromotion(String promotionId, List<String> reasons) {
        public RejectedPromotion {
            Objects.requireNonNull(promotionId, "promotionId");
            reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
        }
    }

    public record Evaluation(
            List<DiscountLine> applied,
            List<RejectedPromotion> rejected,
            Money originalTotal,
            Money totalDiscount,
            Money finalTotal) {
        public Evaluation {
            applied = List.copyOf(Objects.requireNonNull(applied, "applied"));
            rejected = List.copyOf(Objects.requireNonNull(rejected, "rejected"));
            Objects.requireNonNull(originalTotal, "originalTotal");
            Objects.requireNonNull(totalDiscount, "totalDiscount");
            Objects.requireNonNull(finalTotal, "finalTotal");
        }
    }

    public enum RedemptionStatus {
        REDEEMED,
        CANCELLED
    }

    public record Redemption(
            String orderId,
            String customerId,
            Evaluation evaluation,
            Instant redeemedAt,
            RedemptionStatus status,
            Optional<Instant> cancelledAt) {
        public Redemption {
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(customerId, "customerId");
            Objects.requireNonNull(evaluation, "evaluation");
            Objects.requireNonNull(redeemedAt, "redeemedAt");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(cancelledAt, "cancelledAt");
        }
    }

    public record UsageSnapshot(Map<String, Integer> globalUses) {
        public UsageSnapshot {
            globalUses = Map.copyOf(Objects.requireNonNull(globalUses, "globalUses"));
        }
    }

    private record CustomerPromotion(String customerId, String promotionId) {
    }

    private record StoredRedemption(EvaluationContext context, Redemption redemption) {
    }

    private static final Comparator<Promotion> PROMOTION_ORDER =
            Comparator.comparingInt(Promotion::priority).reversed().thenComparing(Promotion::id);

    private final Clock clock;
    private final List<Promotion> promotions;
    private final Map<String, Promotion> promotionsById;
    private final Map<String, Integer> globalUses = new HashMap<>();
    private final Map<CustomerPromotion, Integer> customerUses = new HashMap<>();
    private final Map<String, StoredRedemption> redemptionsByOrder = new HashMap<>();

    public PromotionEngine(Clock clock, List<Promotion> promotions) {
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(promotions, "promotions");
        List<Promotion> sorted = new ArrayList<>(promotions);
        sorted.sort(PROMOTION_ORDER);
        this.promotions = List.copyOf(sorted);

        Map<String, Promotion> byId = new LinkedHashMap<>();
        Set<String> couponCodes = new HashSet<>();
        for (Promotion promotion : sorted) {
            if (byId.putIfAbsent(promotion.id(), promotion) != null) {
                throw new IllegalArgumentException("Duplicate promotion ID: " + promotion.id());
            }
            promotion.couponCode().ifPresent(code -> {
                if (!couponCodes.add(code)) {
                    throw new IllegalArgumentException("Duplicate coupon code: " + code);
                }
            });
        }
        promotionsById = Map.copyOf(byId);
    }

    /** Preview is side-effect free and observes one consistent quota snapshot. */
    public synchronized Evaluation preview(EvaluationContext context) {
        return evaluate(Objects.requireNonNull(context, "context"), clock.instant());
    }

    /** Evaluation plus all quota increments occur under the same atomic boundary. */
    public synchronized Redemption redeem(String orderId, EvaluationContext context) {
        requireNonBlank(orderId, "orderId");
        Objects.requireNonNull(context, "context");
        StoredRedemption existing = redemptionsByOrder.get(orderId);
        if (existing != null) {
            if (!existing.context().equals(context)) {
                throw new PromotionException.IdempotencyConflictException(
                        "Order " + orderId + " was already redeemed with different inputs");
            }
            return existing.redemption();
        }

        Instant now = clock.instant();
        Evaluation evaluation = evaluate(context, now);
        Map<String, Integer> nextGlobalUses = new HashMap<>();
        Map<CustomerPromotion, Integer> nextCustomerUses = new HashMap<>();
        for (DiscountLine line : evaluation.applied()) {
            nextGlobalUses.put(
                    line.promotionId(),
                    Math.incrementExact(globalUses.getOrDefault(line.promotionId(), 0)));
            CustomerPromotion key = new CustomerPromotion(
                    context.customer().id(), line.promotionId());
            nextCustomerUses.put(
                    key, Math.incrementExact(customerUses.getOrDefault(key, 0)));
        }
        globalUses.putAll(nextGlobalUses);
        customerUses.putAll(nextCustomerUses);
        Redemption redemption = new Redemption(
                orderId,
                context.customer().id(),
                evaluation,
                now,
                RedemptionStatus.REDEEMED,
                Optional.empty());
        redemptionsByOrder.put(orderId, new StoredRedemption(context, redemption));
        return redemption;
    }

    /** Cancellation is idempotent and rolls back all counters or none of them. */
    public synchronized Redemption cancel(String orderId) {
        requireNonBlank(orderId, "orderId");
        StoredRedemption stored = redemptionsByOrder.get(orderId);
        if (stored == null) {
            throw new PromotionException("Unknown redemption for order " + orderId);
        }
        if (stored.redemption().status() == RedemptionStatus.CANCELLED) {
            return stored.redemption();
        }
        for (DiscountLine line : stored.redemption().evaluation().applied()) {
            Promotion promotion = promotionsById.get(line.promotionId());
            if (!promotion.usagePolicy().reversible()) {
                throw new PromotionException.RedemptionNotReversibleException(
                        "Promotion " + promotion.id() + " is not reversible");
            }
            requirePositiveCounter(globalUses, line.promotionId());
            requirePositiveCounter(
                    customerUses,
                    new CustomerPromotion(stored.context().customer().id(), line.promotionId()));
        }

        for (DiscountLine line : stored.redemption().evaluation().applied()) {
            decrement(globalUses, line.promotionId());
            decrement(
                    customerUses,
                    new CustomerPromotion(stored.context().customer().id(), line.promotionId()));
        }
        Redemption cancelled = new Redemption(
                stored.redemption().orderId(),
                stored.redemption().customerId(),
                stored.redemption().evaluation(),
                stored.redemption().redeemedAt(),
                RedemptionStatus.CANCELLED,
                Optional.of(clock.instant()));
        redemptionsByOrder.put(orderId, new StoredRedemption(stored.context(), cancelled));
        return cancelled;
    }

    public synchronized UsageSnapshot usageSnapshot() {
        return new UsageSnapshot(new TreeMap<>(globalUses));
    }

    private Evaluation evaluate(EvaluationContext context, Instant now) {
        PricingState pricingState = new PricingState(context.cart());
        List<DiscountLine> applied = new ArrayList<>();
        List<RejectedPromotion> rejected = new ArrayList<>();
        Set<String> usedStackingGroups = new HashSet<>();
        Set<String> knownCouponCodes = new HashSet<>();
        String exclusivePromotion = null;

        for (Promotion promotion : promotions) {
            promotion.couponCode().ifPresent(knownCouponCodes::add);
            if (promotion.couponCode().isPresent()
                    && !context.enteredCouponCodes().contains(promotion.couponCode().orElseThrow())) {
                continue;
            }

            List<String> basicRejections = basicRejections(promotion, context, now);
            if (!basicRejections.isEmpty()) {
                rejected.add(new RejectedPromotion(promotion.id(), basicRejections));
                continue;
            }

            Eligibility eligibility = promotion.condition().evaluate(context);
            if (!eligibility.eligible()) {
                rejected.add(new RejectedPromotion(promotion.id(), eligibility.explanations()));
                continue;
            }

            if (exclusivePromotion != null) {
                rejected.add(new RejectedPromotion(
                        promotion.id(),
                        List.of("Blocked by exclusive promotion " + exclusivePromotion)));
                continue;
            }
            if (promotion.exclusive() && !applied.isEmpty()) {
                rejected.add(new RejectedPromotion(
                        promotion.id(),
                        List.of("Exclusive promotion loses to higher-priority applied promotion(s)")));
                continue;
            }
            if (promotion.stackingGroup().isPresent()
                    && usedStackingGroups.contains(promotion.stackingGroup().orElseThrow())) {
                rejected.add(new RejectedPromotion(
                        promotion.id(),
                        List.of("A higher-priority promotion already won stacking group "
                                + promotion.stackingGroup().orElseThrow())));
                continue;
            }

            BenefitOutcome outcome = promotion.benefit().apply(context, pricingState);
            Money total = outcome.total(context.cart().currency());
            if (total.isZero()) {
                rejected.add(new RejectedPromotion(
                        promotion.id(), List.of("Eligible, but the benefit produced no discount")));
                continue;
            }

            applied.add(new DiscountLine(
                    promotion.id(),
                    promotion.version(),
                    promotion.couponCode(),
                    outcome.explanation(),
                    outcome.components(),
                    total));
            promotion.stackingGroup().ifPresent(usedStackingGroups::add);
            if (promotion.exclusive()) {
                exclusivePromotion = promotion.id();
            }
        }

        context.enteredCouponCodes().stream()
                .filter(code -> !knownCouponCodes.contains(code))
                .sorted()
                .forEach(code -> rejected.add(new RejectedPromotion(
                        "coupon:" + code, List.of("Unknown coupon code"))));

        Money original = context.cart().total();
        Money finalTotal = pricingState.remainingTotal();
        return new Evaluation(
                applied,
                rejected,
                original,
                original.subtract(finalTotal),
                finalTotal);
    }

    private List<String> basicRejections(
            Promotion promotion, EvaluationContext context, Instant now) {
        List<String> reasons = new ArrayList<>();
        if (promotion.lifecycle() != Promotion.Lifecycle.ACTIVE) {
            reasons.add("Promotion is " + promotion.lifecycle().name().toLowerCase());
        }
        if (now.isBefore(promotion.activeFrom())) {
            reasons.add("Promotion is not active yet");
        }
        if (!now.isBefore(promotion.activeUntil())) {
            reasons.add("Promotion expired at " + promotion.activeUntil());
        }
        int globalLimit = promotion.usagePolicy().globalLimit();
        if (globalLimit > 0 && globalUses.getOrDefault(promotion.id(), 0) >= globalLimit) {
            reasons.add("Global redemption limit is exhausted");
        }
        int customerLimit = promotion.usagePolicy().perCustomerLimit();
        CustomerPromotion key = new CustomerPromotion(context.customer().id(), promotion.id());
        if (customerLimit > 0 && customerUses.getOrDefault(key, 0) >= customerLimit) {
            reasons.add("Customer redemption limit is exhausted");
        }
        return List.copyOf(reasons);
    }

    private static <K> void decrement(Map<K, Integer> counters, K key) {
        int current = counters.get(key);
        if (current == 1) {
            counters.remove(key);
        } else {
            counters.put(key, current - 1);
        }
    }

    private static <K> void requirePositiveCounter(Map<K, Integer> counters, K key) {
        Integer current = counters.get(key);
        if (current == null || current <= 0) {
            throw new IllegalStateException("Redemption counter invariant violated for " + key);
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
