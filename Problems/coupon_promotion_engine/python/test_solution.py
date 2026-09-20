from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from threading import Barrier
import unittest
from typing import Optional

from solution import (
    Cart,
    CartLine,
    EvaluationContext,
    FixedBenefit,
    IdempotencyConflict,
    LimitExceeded,
    MinimumSubtotal,
    Money,
    PercentageBenefit,
    Promotion,
    PromotionEngine,
    QuoteStale,
    UsagePolicy,
)


NOW = datetime(2026, 8, 11, 10, tzinfo=timezone.utc)


def usd(value: str) -> Money:
    return Money(Decimal(value), "USD")


def context(
    customer: str = "customer-1", *, total: Optional[str] = None
) -> EvaluationContext:
    lines = (
        (CartLine("only", 1, usd(total)),)
        if total is not None
        else (CartLine("b", 1, usd("50")), CartLine("a", 1, usd("70")))
    )
    return EvaluationContext(Cart(lines, usd("0" if total else "8")), customer, NOW)


class Clock:
    def __init__(self) -> None:
        self.value = NOW

    def __call__(self) -> datetime:
        return self.value

    def advance(self, duration: timedelta) -> None:
        self.value += duration


def engine(clock=lambda: NOW) -> PromotionEngine:
    start, end = NOW - timedelta(days=1), NOW + timedelta(days=1)
    return PromotionEngine(
        (
            Promotion(
                "SAVE20",
                3,
                start,
                end,
                FixedBenefit(usd("20")),
                (MinimumSubtotal(usd("100")),),
                "SAVE20",
                False,
                80,
                "ORDER",
                UsagePolicy(global_limit=1, per_customer_limit=1),
            ),
            Promotion(
                "AUTO10", 7, start, end, PercentageBenefit(1_000), priority=50,
                stacking_group="ORDER",
            ),
            Promotion("BONUS8", 2, start, end, FixedBenefit(usd("8")), priority=20),
        ),
        clock=clock,
    )


class PromotionEngineTests(unittest.TestCase):
    def test_preview_is_pure_deterministic_and_stacks_by_policy(self) -> None:
        subject = engine()
        quote = subject.preview(context(), (" save20 ", "SAVE20"))
        repeat = subject.preview(context(), ("SAVE20",))

        self.assertEqual(quote.quote_id, repeat.quote_id)
        self.assertEqual(quote.payable, usd("100"))
        self.assertEqual(quote.applied_campaigns, (("SAVE20", 3), ("BONUS8", 2)))
        self.assertEqual(subject.usage_count("SAVE20", 3), 0)

    def test_percentage_rounds_to_minor_units_and_never_overdiscounts(self) -> None:
        start, end = NOW - timedelta(1), NOW + timedelta(1)
        subject = PromotionEngine(
            (Promotion("PERCENT", 1, start, end, PercentageBenefit(1_000)),),
            clock=lambda: NOW,
        )
        self.assertEqual(subject.preview(context(total="0.05")).payable, usd("0.04"))

        fixed = PromotionEngine(
            (Promotion("HUGE", 1, start, end, FixedBenefit(usd("99"))),),
            clock=lambda: NOW,
        )
        self.assertEqual(fixed.preview(context(total="1")).payable, usd("0"))

    def test_one_checkout_wins_last_use_and_lost_response_replays(self) -> None:
        subject = engine()
        contexts = (context("a"), context("b"))
        quotes = tuple(subject.preview(item, ("SAVE20",)) for item in contexts)
        barrier = Barrier(2)

        def compete(index: int):
            barrier.wait()
            try:
                return subject.redeem(
                    f"order-{index}", quotes[index], contexts[index], f"key-{index}", ("SAVE20",)
                )
            except LimitExceeded as error:
                return error

        with ThreadPoolExecutor(max_workers=2) as pool:
            outcomes = list(pool.map(compete, (0, 1)))
        winners = [item for item in outcomes if not isinstance(item, Exception)]
        self.assertEqual(len(winners), 1)
        self.assertEqual(subject.usage_count("SAVE20", 3), 1)

        winner = winners[0]
        index = int(winner.order_id[-1])
        replay = subject.redeem(
            winner.order_id, quotes[index], contexts[index], f"key-{index}", ("SAVE20",)
        )
        self.assertTrue(replay.replayed)
        self.assertEqual(replay.redemption_id, winner.redemption_id)
        self.assertEqual(subject.usage_count("SAVE20", 3), 1)

    def test_request_hash_conflict_and_quote_binding(self) -> None:
        subject = engine()
        first = context("a")
        quote = subject.preview(first, ("SAVE20",))
        subject.redeem("order-a", quote, first, "shared", ("SAVE20",))
        with self.assertRaises(IdempotencyConflict):
            subject.redeem("different-order", quote, first, "shared", ("SAVE20",))
        with self.assertRaises(QuoteStale):
            subject.redeem("order-b", quote, context("b"), "fresh", ("SAVE20",))

        clock = Clock()
        expiring = engine(clock)
        expiring_quote = expiring.preview(first, ("SAVE20",))
        clock.advance(timedelta(minutes=5))
        with self.assertRaises(QuoteStale):
            expiring.redeem("late", expiring_quote, first, "late-key", ("SAVE20",))

        campaign_end = NOW + timedelta(seconds=1)
        short_campaign = PromotionEngine(
            (Promotion("SHORT", 1, NOW - timedelta(1), campaign_end, FixedBenefit(usd("1"))),),
            clock=clock,
        )
        clock.value = NOW
        short_quote = short_campaign.preview(first)
        self.assertEqual(campaign_end, short_quote.expires_at)
        clock.advance(timedelta(seconds=1))
        with self.assertRaises(QuoteStale):
            short_campaign.redeem("after-campaign", short_quote, first, "short-key")

    def test_reversal_restores_quota_once(self) -> None:
        subject = engine()
        ctx = context()
        quote = subject.preview(ctx, ("SAVE20",))
        subject.redeem("order-7", quote, ctx, "redeem-7", ("SAVE20",))
        subject.reverse("order-7", "CANCELLED", "reverse-7")
        replay = subject.reverse("order-7", "CANCELLED", "reverse-7")
        self.assertTrue(replay.replayed)
        self.assertEqual(subject.usage_count("SAVE20", 3), 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
