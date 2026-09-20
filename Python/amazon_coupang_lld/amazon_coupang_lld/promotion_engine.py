from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from threading import RLock
from typing import Protocol

from .support import Money


@dataclass(frozen=True)
class CartLine:
    sku: str
    quantity: int
    unit_price: Money

    @property
    def subtotal(self) -> int:
        return self.quantity * self.unit_price.minor_units


@dataclass(frozen=True)
class Context:
    lines: tuple[CartLine, ...]
    customer_tier: str
    coupons: frozenset[str]
    now: datetime

    @property
    def subtotal(self) -> int:
        return sum(line.subtotal for line in self.lines)

    @property
    def currency(self) -> str:
        return self.lines[0].unit_price.currency


class Condition(Protocol):
    def matches(self, context: Context) -> bool: ...


class Benefit(Protocol):
    def discount(self, context: Context, remaining: int) -> tuple[int, str]: ...


@dataclass(frozen=True)
class MinimumSpend:
    amount: int
    def matches(self, context: Context) -> bool:
        return context.subtotal >= self.amount


@dataclass(frozen=True)
class Coupon:
    code: str
    def matches(self, context: Context) -> bool:
        return self.code in context.coupons


@dataclass(frozen=True)
class CustomerTier:
    tier: str
    def matches(self, context: Context) -> bool:
        return self.tier == context.customer_tier


@dataclass(frozen=True)
class AllOf:
    conditions: tuple[Condition, ...]
    def matches(self, context: Context) -> bool:
        return all(condition.matches(context) for condition in self.conditions)


@dataclass(frozen=True)
class PercentageOff:
    percent: int
    def discount(self, context: Context, remaining: int) -> tuple[int, str]:
        return remaining * self.percent // 100, f"{self.percent}% off"


@dataclass(frozen=True)
class FixedOff:
    amount: int
    def discount(self, context: Context, remaining: int) -> tuple[int, str]:
        return self.amount, "fixed discount"


@dataclass(frozen=True)
class Promotion:
    id: str
    priority: int
    exclusive: bool
    condition: Condition
    benefit: Benefit
    usage_limit: int


@dataclass(frozen=True)
class Adjustment:
    promotion_id: str
    discount: Money
    explanation: str


@dataclass(frozen=True)
class Quote:
    subtotal: Money
    total: Money
    adjustments: tuple[Adjustment, ...]


class PromotionEngine:
    def __init__(self, promotions: list[Promotion]) -> None:
        self._promotions = tuple(promotions)
        self._lock = RLock()
        self._usage: dict[str, int] = {promotion.id: 0 for promotion in promotions}
        self._redemptions: dict[str, Quote] = {}

    def preview(self, context: Context) -> Quote:
        with self._lock:
            return self._evaluate(context, consume=False)

    def redeem(self, order_id: str, context: Context) -> Quote:
        with self._lock:
            if order_id in self._redemptions:
                return self._redemptions[order_id]
            quote = self._evaluate(context, consume=True)
            self._redemptions[order_id] = quote
            return quote

    def _evaluate(self, context: Context, consume: bool) -> Quote:
        eligible = [promotion for promotion in self._promotions
                    if promotion.condition.matches(context)
                    and self._usage[promotion.id] < promotion.usage_limit]
        eligible.sort(key=lambda promotion: (-promotion.priority, promotion.id))
        exclusive = next((promotion for promotion in eligible if promotion.exclusive), None)
        selected = [exclusive] if exclusive else eligible
        remaining = context.subtotal
        adjustments: list[Adjustment] = []
        for promotion in selected:
            raw, explanation = promotion.benefit.discount(context, remaining)
            discount = max(0, min(remaining, raw))
            if discount:
                adjustments.append(Adjustment(promotion.id,
                                              Money(discount, context.currency), explanation))
                remaining -= discount
                if consume:
                    self._usage[promotion.id] += 1
        return Quote(Money(context.subtotal, context.currency),
                     Money(remaining, context.currency), tuple(adjustments))


def run_demo() -> None:
    engine = PromotionEngine([
        Promotion("SAVE10", 100, False,
                  AllOf((Coupon("SAVE10"), MinimumSpend(2_000))), PercentageOff(10), 1_000),
        Promotion("VIP500", 50, False, CustomerTier("VIP"), FixedOff(500), 1_000),
    ])
    context = Context((CartLine("A", 2, Money(1_500, "USD")),), "VIP",
                      frozenset({"SAVE10"}), datetime.now(timezone.utc))
    first = engine.redeem("order-1", context)
    duplicate = engine.redeem("order-1", context)
    assert first is duplicate and first.total == Money(2_200, "USD")
    print("Python Promotion Engine: passed")
