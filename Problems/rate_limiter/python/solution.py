"""Thread-safe, interview-sized rate limiter with pluggable policies."""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum
from threading import Lock, RLock
from time import monotonic_ns
from typing import Callable, Union


class PolicyType(str, Enum):
    TOKEN_BUCKET = "token_bucket"
    FIXED_WINDOW = "fixed_window"


@dataclass(frozen=True)
class RateLimitRule:
    rule_id: str
    capacity: int
    period_ns: int
    policy_type: PolicyType
    version: int = 1
    burst_allowance: int = 0

    def __post_init__(self) -> None:
        if not self.rule_id.strip():
            raise ValueError("rule_id must not be blank")
        if self.capacity <= 0 or self.period_ns <= 0:
            raise ValueError("capacity and period_ns must be positive")
        if self.burst_allowance < 0 or self.version <= 0:
            raise ValueError("burst_allowance must be non-negative and version positive")

    @property
    def maximum_balance(self) -> int:
        return self.capacity + self.burst_allowance


@dataclass(frozen=True)
class RateLimitKey:
    tenant: str
    principal: str
    endpoint: str

    def __post_init__(self) -> None:
        if not all(value.strip() for value in (self.tenant, self.principal, self.endpoint)):
            raise ValueError("rate-limit key parts must not be blank")


@dataclass(frozen=True)
class RateLimitRequest:
    rule_id: str
    key: RateLimitKey
    cost: int = 1


@dataclass(frozen=True)
class RateLimitDecision:
    allowed: bool
    remaining: int
    retry_after_ns: int
    reset_after_ns: int
    rule_version: int
    reason: str


class ManualClock:
    """Deterministic monotonic clock used by examples and tests."""

    def __init__(self, initial_ns: int = 0) -> None:
        self._now_ns = initial_ns
        self._lock = Lock()

    def now_ns(self) -> int:
        with self._lock:
            return self._now_ns

    def advance(self, nanoseconds: int) -> None:
        if nanoseconds < 0:
            raise ValueError("a monotonic clock cannot move backwards")
        with self._lock:
            self._now_ns += nanoseconds


@dataclass
class _TokenBucketState:
    credits: int
    last_refill_ns: int


@dataclass
class _FixedWindowState:
    used: int
    window_start_ns: int


PolicyState = Union[_TokenBucketState, _FixedWindowState]


class RateLimitPolicy(ABC):
    @abstractmethod
    def initial_state(self, rule: RateLimitRule, now_ns: int) -> PolicyState:
        raise NotImplementedError

    @abstractmethod
    def evaluate(
        self,
        rule: RateLimitRule,
        state: PolicyState,
        cost: int,
        now_ns: int,
    ) -> tuple[PolicyState, RateLimitDecision]:
        raise NotImplementedError


class TokenBucketPolicy(RateLimitPolicy):
    """Integer token-nanosecond credits avoid floating-point refill drift."""

    def initial_state(self, rule: RateLimitRule, now_ns: int) -> PolicyState:
        return _TokenBucketState(rule.maximum_balance * rule.period_ns, now_ns)

    def evaluate(
        self,
        rule: RateLimitRule,
        state: PolicyState,
        cost: int,
        now_ns: int,
    ) -> tuple[PolicyState, RateLimitDecision]:
        if not isinstance(state, _TokenBucketState):
            raise TypeError("token bucket received incompatible state")
        elapsed = max(0, now_ns - state.last_refill_ns)
        cap = rule.maximum_balance * rule.period_ns
        credits = min(cap, state.credits + elapsed * rule.capacity)
        required = cost * rule.period_ns
        allowed = credits >= required
        if allowed:
            credits -= required

        remaining = credits // rule.period_ns
        missing = max(0, required - credits)
        retry_after = 0 if allowed else (missing + rule.capacity - 1) // rule.capacity
        full_missing = max(0, cap - credits)
        reset_after = (full_missing + rule.capacity - 1) // rule.capacity
        decision = RateLimitDecision(
            allowed=allowed,
            remaining=remaining,
            retry_after_ns=retry_after,
            reset_after_ns=reset_after,
            rule_version=rule.version,
            reason="allowed" if allowed else "quota_exhausted",
        )
        return _TokenBucketState(credits, now_ns), decision


class FixedWindowPolicy(RateLimitPolicy):
    def initial_state(self, rule: RateLimitRule, now_ns: int) -> PolicyState:
        return _FixedWindowState(0, now_ns)

    def evaluate(
        self,
        rule: RateLimitRule,
        state: PolicyState,
        cost: int,
        now_ns: int,
    ) -> tuple[PolicyState, RateLimitDecision]:
        if not isinstance(state, _FixedWindowState):
            raise TypeError("fixed window received incompatible state")
        elapsed = max(0, now_ns - state.window_start_ns)
        if elapsed >= rule.period_ns:
            skipped = elapsed // rule.period_ns
            state = _FixedWindowState(0, state.window_start_ns + skipped * rule.period_ns)
        limit = rule.maximum_balance
        allowed = state.used + cost <= limit
        used = state.used + cost if allowed else state.used
        until_reset = max(0, state.window_start_ns + rule.period_ns - now_ns)
        decision = RateLimitDecision(
            allowed=allowed,
            remaining=limit - used,
            retry_after_ns=0 if allowed else until_reset,
            reset_after_ns=until_reset,
            rule_version=rule.version,
            reason="allowed" if allowed else "quota_exhausted",
        )
        return _FixedWindowState(used, state.window_start_ns), decision


@dataclass
class _Cell:
    rule_version: int
    state: PolicyState
    last_seen_ns: int
    lock: RLock


class RateLimiter:
    """Coordinator: configuration, per-key cells, atomic decisions, and cleanup."""

    def __init__(self, clock: Callable[[], int] = monotonic_ns) -> None:
        self._clock = clock
        self._rules: dict[str, RateLimitRule] = {}
        self._cells: dict[tuple[str, RateLimitKey], _Cell] = {}
        self._policies: dict[PolicyType, RateLimitPolicy] = {
            PolicyType.TOKEN_BUCKET: TokenBucketPolicy(),
            PolicyType.FIXED_WINDOW: FixedWindowPolicy(),
        }
        self._registry_lock = RLock()

    def register_rule(self, rule: RateLimitRule) -> None:
        with self._registry_lock:
            if rule.rule_id in self._rules:
                raise ValueError(f"duplicate rule: {rule.rule_id}")
            self._rules[rule.rule_id] = rule

    def replace_rule(self, rule_id: str, expected_version: int, replacement: RateLimitRule) -> None:
        if rule_id != replacement.rule_id:
            raise ValueError("replacement must preserve rule_id")
        with self._registry_lock:
            current = self._rules.get(rule_id)
            if current is None:
                raise KeyError(f"unknown rule: {rule_id}")
            if current.version != expected_version:
                raise ValueError(
                    f"rule version conflict: expected {expected_version}, actual {current.version}"
                )
            if replacement.version != expected_version + 1:
                raise ValueError("replacement version must increase by exactly one")
            self._rules[rule_id] = replacement

    def allow(self, request: RateLimitRequest) -> RateLimitDecision:
        cell_key = (request.rule_id, request.key)
        while True:
            now_ns = self._clock()
            with self._registry_lock:
                rule = self._rules.get(request.rule_id)
                if rule is None:
                    raise KeyError(f"unknown rule: {request.rule_id}")
                if request.cost <= 0 or request.cost > rule.maximum_balance:
                    raise ValueError(
                        "cost must be positive and no greater than maximum balance"
                    )
                policy = self._policies[rule.policy_type]
                cell = self._cells.get(cell_key)
                if cell is None:
                    cell = _Cell(
                        rule_version=rule.version,
                        state=policy.initial_state(rule, now_ns),
                        last_seen_ns=now_ns,
                        lock=RLock(),
                    )
                    self._cells[cell_key] = cell

            with cell.lock:
                # Eviction may have removed the resolved cell while this caller was
                # waiting for its lock. Never spend from that orphan: retry lookup.
                with self._registry_lock:
                    if self._cells.get(cell_key) is not cell:
                        continue
                    # Re-resolve configuration at the mutation point so a caller
                    # cannot reset a cell back to an older rule after replacement.
                    rule = self._rules[request.rule_id]
                    if request.cost > rule.maximum_balance:
                        raise ValueError(
                            "cost must be positive and no greater than maximum balance"
                        )
                    policy = self._policies[rule.policy_type]
                # Sample elapsed time at the serialized mutation point. Sampling
                # before a contended lock can make last_seen/refill move backwards.
                now_ns = max(self._clock(), cell.last_seen_ns)
                if cell.rule_version != rule.version:
                    cell.rule_version = rule.version
                    cell.state = policy.initial_state(rule, now_ns)
                cell.state, decision = policy.evaluate(
                    rule, cell.state, request.cost, now_ns
                )
                cell.last_seen_ns = now_ns
                return decision

    def evict_idle(self, idle_for_ns: int) -> int:
        if idle_for_ns < 0:
            raise ValueError("idle_for_ns must be non-negative")
        now_ns = self._clock()
        with self._registry_lock:
            candidates = tuple(self._cells.items())

        evicted = 0
        for key, cell in candidates:
            # Do not hold the registry lock while waiting for a hot cell. The
            # identity check below makes the optimistic candidate snapshot safe.
            with cell.lock:
                with self._registry_lock:
                    if (
                        self._cells.get(key) is cell
                        and now_ns - cell.last_seen_ns >= idle_for_ns
                    ):
                        del self._cells[key]
                        evicted += 1
        return evicted
