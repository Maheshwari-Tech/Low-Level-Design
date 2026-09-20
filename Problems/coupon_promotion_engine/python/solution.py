"""One-hour coupon engine: pure quotes and atomic, idempotent redemption."""

from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP
from hashlib import sha256
import json
from threading import RLock
from typing import Callable, Optional, Protocol


_CENT = Decimal("0.01")


class PromotionError(Exception): ...
class IdempotencyConflict(PromotionError): ...
class QuoteStale(PromotionError): ...
class LimitExceeded(PromotionError): ...
class OrderAlreadyRedeemed(PromotionError): ...
class NonReversible(PromotionError): ...


@dataclass(frozen=True)
class Money:
    amount: Decimal
    currency: str = "USD"

    def __post_init__(self) -> None:
        amount = Decimal(str(self.amount)).quantize(_CENT, rounding=ROUND_HALF_UP)
        if amount < 0 or not self.currency.strip():
            raise ValueError("money must be non-negative and have a currency")
        object.__setattr__(self, "amount", amount)
        object.__setattr__(self, "currency", self.currency.upper())

    @classmethod
    def from_minor(cls, units: int, currency: str) -> Money:
        if units < 0:
            raise ValueError("minor units cannot be negative")
        return cls(Decimal(units) / 100, currency)

    @property
    def minor_units(self) -> int:
        return int(self.amount * 100)

    def check_currency(self, other: Money) -> None:
        if self.currency != other.currency:
            raise ValueError("currency mismatch")

    def __add__(self, other: Money) -> Money:
        self.check_currency(other)
        return Money(self.amount + other.amount, self.currency)

    def __sub__(self, other: Money) -> Money:
        self.check_currency(other)
        if other.amount > self.amount:
            raise ValueError("money subtraction would be negative")
        return Money(self.amount - other.amount, self.currency)


@dataclass(frozen=True)
class CartLine:
    line_id: str
    quantity: int
    unit_price: Money

    def __post_init__(self) -> None:
        if not self.line_id.strip() or self.quantity <= 0:
            raise ValueError("line ID and positive quantity are required")

    @property
    def total(self) -> Money:
        return Money(self.unit_price.amount * self.quantity, self.unit_price.currency)


@dataclass(frozen=True)
class Cart:
    lines: tuple[CartLine, ...]
    shipping: Money

    def __post_init__(self) -> None:
        if not self.lines or len({line.line_id for line in self.lines}) != len(self.lines):
            raise ValueError("cart needs lines with unique IDs")
        if any(line.unit_price.currency != self.shipping.currency for line in self.lines):
            raise ValueError("cart must use one currency")

    @property
    def subtotal(self) -> Money:
        units = sum(line.total.minor_units for line in self.lines)
        return Money.from_minor(units, self.shipping.currency)

    @property
    def total(self) -> Money:
        return self.subtotal + self.shipping


@dataclass(frozen=True)
class EvaluationContext:
    cart: Cart
    customer_id: str
    now: datetime

    def __post_init__(self) -> None:
        if not self.customer_id.strip() or self.now.tzinfo is None:
            raise ValueError("customer and timezone-aware evaluation time are required")

    def fingerprint(self) -> str:
        return _digest(
            {
                "customer": self.customer_id,
                "at": self.now.isoformat(),
                "shipping": self.cart.shipping.minor_units,
                "currency": self.cart.shipping.currency,
                "lines": sorted(
                    (line.line_id, line.quantity, line.unit_price.minor_units)
                    for line in self.cart.lines
                ),
            }
        )


class Condition(Protocol):
    def allows(self, context: EvaluationContext) -> bool: ...


@dataclass(frozen=True)
class MinimumSubtotal:
    minimum: Money

    def allows(self, context: EvaluationContext) -> bool:
        context.cart.subtotal.check_currency(self.minimum)
        return context.cart.subtotal.amount >= self.minimum.amount


class Benefit(Protocol):
    def discount_minor(self, remaining: int, currency: str) -> int: ...


@dataclass(frozen=True)
class FixedBenefit:
    amount: Money

    def discount_minor(self, remaining: int, currency: str) -> int:
        if self.amount.currency != currency:
            raise ValueError("currency mismatch")
        return min(remaining, self.amount.minor_units)


@dataclass(frozen=True)
class PercentageBenefit:
    basis_points: int
    cap: Optional[Money] = None

    def __post_init__(self) -> None:
        if not 0 < self.basis_points <= 10_000:
            raise ValueError("basis points must be in (0, 10000]")

    def discount_minor(self, remaining: int, currency: str) -> int:
        units = int(
            (Decimal(remaining) * self.basis_points / 10_000).quantize(
                Decimal("1"), rounding=ROUND_HALF_UP
            )
        )
        if self.cap is not None:
            if self.cap.currency != currency:
                raise ValueError("currency mismatch")
            units = min(units, self.cap.minor_units)
        return min(units, remaining)


@dataclass(frozen=True)
class UsagePolicy:
    global_limit: int = 0
    per_customer_limit: int = 0
    reversible: bool = True

    def __post_init__(self) -> None:
        if self.global_limit < 0 or self.per_customer_limit < 0:
            raise ValueError("usage limits cannot be negative")


@dataclass(frozen=True)
class Promotion:
    promotion_id: str
    version: int
    active_from: datetime
    active_until: datetime
    benefit: Benefit
    conditions: tuple[Condition, ...] = ()
    coupon_code: Optional[str] = None
    automatic: bool = True
    priority: int = 0
    stacking_group: Optional[str] = None
    usage: UsagePolicy = UsagePolicy()

    def __post_init__(self) -> None:
        if not self.promotion_id.strip() or self.version <= 0:
            raise ValueError("promotion identity/version is invalid")
        if self.active_from.tzinfo is None or self.active_until.tzinfo is None:
            raise ValueError("promotion times must be timezone-aware")
        if self.active_from >= self.active_until:
            raise ValueError("promotion validity window is empty")
        code = (self.coupon_code or "").strip().upper() or None
        if not self.automatic and code is None:
            raise ValueError("entered promotions need a coupon code")
        object.__setattr__(self, "coupon_code", code)

    @property
    def key(self) -> tuple[str, int]:
        return self.promotion_id, self.version


@dataclass(frozen=True)
class Discount:
    promotion_id: str
    promotion_version: int
    amount: Money


@dataclass(frozen=True)
class Quote:
    quote_id: str
    context_fingerprint: str
    normalized_codes: tuple[str, ...]
    expires_at: datetime
    applied: tuple[Discount, ...]
    payable: Money

    @property
    def applied_campaigns(self) -> tuple[tuple[str, int], ...]:
        return tuple((item.promotion_id, item.promotion_version) for item in self.applied)


@dataclass(frozen=True)
class Redemption:
    redemption_id: str
    order_id: str
    quote_id: str
    customer_id: str
    applied: tuple[Discount, ...]
    payable: Money
    status: str = "COMMITTED"
    replayed: bool = False


@dataclass(frozen=True)
class _Stored:
    request_hash: str
    result: Redemption


class PromotionEngine:
    """The lock represents one serializable usage/idempotency transaction."""

    def __init__(
        self,
        promotions: tuple[Promotion, ...],
        *,
        quote_valid_for: timedelta = timedelta(minutes=5),
        clock: Optional[Callable[[], datetime]] = None,
    ) -> None:
        if quote_valid_for <= timedelta(0):
            raise ValueError("quote validity must be positive")
        if len({item.key for item in promotions}) != len(promotions):
            raise ValueError("promotion versions must be unique")
        codes = [item.coupon_code for item in promotions if item.coupon_code]
        if len(set(codes)) != len(codes):
            raise ValueError("coupon codes must be unique")
        self._promotions = promotions
        self._by_key = {item.key: item for item in promotions}
        self._clock = clock or (lambda: datetime.now(timezone.utc))
        if self._clock().tzinfo is None:
            raise ValueError("clock must return timezone-aware datetimes")
        self._quote_valid_for = quote_valid_for
        self._global: dict[tuple[str, int], int] = {}
        self._customer: dict[tuple[tuple[str, int], str], int] = {}
        self._orders: dict[str, Redemption] = {}
        self._idempotency: dict[tuple[str, str], _Stored] = {}
        self._lock = RLock()

    def preview(
        self, context: EvaluationContext, entered_codes: tuple[str, ...] = ()
    ) -> Quote:
        return self._price(context, self._normalize(entered_codes))

    def redeem(
        self,
        order_id: str,
        quote: Quote,
        context: EvaluationContext,
        idempotency_key: str,
        entered_codes: tuple[str, ...] = (),
    ) -> Redemption:
        if not order_id.strip() or not idempotency_key.strip():
            raise ValueError("order and idempotency keys are required")
        codes = self._normalize(entered_codes)
        request_hash = _digest((order_id, quote.quote_id, context.fingerprint(), codes))
        operation = ("REDEEM", idempotency_key)
        with self._lock:
            replay = self._replay(operation, request_hash)
            if replay is not None:
                return replay
            if order_id in self._orders:
                raise OrderAlreadyRedeemed(order_id)
            if context.fingerprint() != quote.context_fingerprint or codes != quote.normalized_codes:
                raise QuoteStale("quote does not match checkout inputs")
            if self._clock() >= quote.expires_at:
                raise QuoteStale("quote expired")
            current = self._price(context, codes)
            if self._signature(current) != self._signature(quote):
                raise QuoteStale("promotion pricing changed")
            for key in quote.applied_campaigns:
                reason = self._limit_reason(self._by_key[key], context.customer_id)
                if reason:
                    raise LimitExceeded(reason)
            for key in quote.applied_campaigns:
                self._global[key] = self._global.get(key, 0) + 1
                customer_key = (key, context.customer_id)
                self._customer[customer_key] = self._customer.get(customer_key, 0) + 1
            result = Redemption(
                "red-" + _digest((order_id, quote.quote_id))[:16],
                order_id,
                quote.quote_id,
                context.customer_id,
                quote.applied,
                quote.payable,
            )
            self._orders[order_id] = result
            self._idempotency[operation] = _Stored(request_hash, result)
            return result

    def reverse(self, order_id: str, reason: str, idempotency_key: str) -> Redemption:
        if not all((order_id.strip(), reason.strip(), idempotency_key.strip())):
            raise ValueError("reversal fields are required")
        request_hash = _digest((order_id, reason))
        operation = ("REVERSE", idempotency_key)
        with self._lock:
            replay = self._replay(operation, request_hash)
            if replay is not None:
                return replay
            current = self._orders.get(order_id)
            if current is None:
                raise KeyError(order_id)
            if current.status == "REVERSED":
                result = replace(current, replayed=True)
            else:
                campaign_keys = tuple(
                    (item.promotion_id, item.promotion_version) for item in current.applied
                )
                for key in campaign_keys:
                    if not self._by_key[key].usage.reversible:
                        raise NonReversible(key)
                for key in campaign_keys:
                    self._global[key] -= 1
                    self._customer[(key, current.customer_id)] -= 1
                result = replace(current, status="REVERSED")
                self._orders[order_id] = result
            self._idempotency[operation] = _Stored(request_hash, result)
            return result

    def usage_count(self, promotion_id: str, version: int) -> int:
        with self._lock:
            return self._global.get((promotion_id, version), 0)

    def _price(self, context: EvaluationContext, codes: tuple[str, ...]) -> Quote:
        by_code = {item.coupon_code: item for item in self._promotions if item.coupon_code}
        candidates = {item.key: item for item in self._promotions if item.automatic}
        candidates.update(
            (promotion.key, promotion)
            for code in codes
            if (promotion := by_code.get(code)) is not None
        )
        remaining = context.cart.total.minor_units
        groups: set[str] = set()
        applied: list[Discount] = []
        for promotion in sorted(
            candidates.values(), key=lambda item: (-item.priority, item.promotion_id)
        ):
            if not (promotion.active_from <= context.now < promotion.active_until):
                continue
            if not all(condition.allows(context) for condition in promotion.conditions):
                continue
            if promotion.stacking_group and promotion.stacking_group in groups:
                continue
            units = promotion.benefit.discount_minor(
                remaining, context.cart.shipping.currency
            )
            if not units:
                continue
            remaining -= units
            applied.append(
                Discount(*promotion.key, Money.from_minor(units, context.cart.shipping.currency))
            )
            if promotion.stacking_group:
                groups.add(promotion.stacking_group)
        issued_at = self._clock()
        signature = (context.fingerprint(), codes, self._discount_signature(applied), remaining)
        # A quote cannot promise an applied campaign beyond its immutable window.
        campaign_ends = (self._by_key[item.promotion_id, item.promotion_version].active_until
                         for item in applied)
        expires_at = min((issued_at + self._quote_valid_for, *campaign_ends))
        return Quote(
            "quote-" + _digest((signature, issued_at.isoformat()))[:16],
            context.fingerprint(),
            codes,
            expires_at,
            tuple(applied),
            Money.from_minor(remaining, context.cart.shipping.currency),
        )

    def _limit_reason(self, promotion: Promotion, customer_id: str) -> Optional[str]:
        if promotion.usage.global_limit and self._global.get(promotion.key, 0) >= promotion.usage.global_limit:
            return "GLOBAL_LIMIT_EXHAUSTED"
        customer_key = (promotion.key, customer_id)
        if promotion.usage.per_customer_limit and self._customer.get(customer_key, 0) >= promotion.usage.per_customer_limit:
            return "CUSTOMER_LIMIT_EXHAUSTED"
        return None

    def _replay(self, operation: tuple[str, str], request_hash: str) -> Optional[Redemption]:
        stored = self._idempotency.get(operation)
        if stored is None:
            return None
        if stored.request_hash != request_hash:
            raise IdempotencyConflict("idempotency key reused for another request")
        return replace(stored.result, replayed=True)

    @staticmethod
    def _normalize(codes: tuple[str, ...]) -> tuple[str, ...]:
        return tuple(sorted({code.strip().upper() for code in codes if code.strip()}))

    @staticmethod
    def _discount_signature(discounts: list[Discount] | tuple[Discount, ...]) -> tuple[object, ...]:
        return tuple(
            (item.promotion_id, item.promotion_version, item.amount.minor_units)
            for item in discounts
        )

    @classmethod
    def _signature(cls, quote: Quote) -> tuple[object, ...]:
        return cls._discount_signature(quote.applied), quote.payable.minor_units


def _digest(value: object) -> str:
    return sha256(
        json.dumps(value, sort_keys=True, separators=(",", ":"), default=str).encode()
    ).hexdigest()
