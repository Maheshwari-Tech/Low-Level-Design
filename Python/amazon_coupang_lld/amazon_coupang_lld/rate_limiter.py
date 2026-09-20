from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from threading import Lock
from time import monotonic_ns
from typing import Callable, Protocol


@dataclass(frozen=True)
class Request:
    tenant_id: str
    user_id: str
    endpoint: str
    cost: int = 1


@dataclass(frozen=True)
class Policy:
    capacity: int
    tokens_per_second: float


@dataclass(frozen=True)
class Decision:
    allowed: bool
    remaining: int
    retry_after_seconds: float


class PolicyResolver(Protocol):
    def resolve(self, request: Request) -> Policy: ...


@dataclass
class _Bucket:
    tokens: float
    last_refill_ns: int
    lock: Lock = field(default_factory=Lock)


class TokenBucket:
    def __init__(self, clock_ns: Callable[[], int] = monotonic_ns) -> None:
        self._clock_ns = clock_ns
        self._buckets: dict[str, _Bucket] = {}
        self._map_lock = Lock()

    def allow(self, key: str, cost: int, policy: Policy) -> Decision:
        now = self._clock_ns()
        with self._map_lock:
            bucket = self._buckets.setdefault(key, _Bucket(float(policy.capacity), now))
        with bucket.lock:
            elapsed = max(0, now - bucket.last_refill_ns) / 1_000_000_000
            bucket.tokens = min(policy.capacity,
                                bucket.tokens + elapsed * policy.tokens_per_second)
            bucket.last_refill_ns = now
            if bucket.tokens >= cost:
                bucket.tokens -= cost
                return Decision(True, int(bucket.tokens), 0.0)
            missing = cost - bucket.tokens
            return Decision(False, int(bucket.tokens), missing / policy.tokens_per_second)


class RateLimiter:
    def __init__(self, bucket: TokenBucket, resolver: PolicyResolver) -> None:
        self._bucket = bucket
        self._resolver = resolver

    def allow(self, request: Request) -> Decision:
        policy = self._resolver.resolve(request)
        key = f"{request.tenant_id}:{request.user_id}:{request.endpoint}"
        return self._bucket.allow(key, request.cost, policy)


class _Resolver:
    def resolve(self, request: Request) -> Policy:
        return Policy(10, 5.0)


def run_demo() -> None:
    now = 0

    def clock() -> int:
        return now

    limiter = RateLimiter(TokenBucket(clock), _Resolver())
    request = Request("tenant", "user", "/orders")
    with ThreadPoolExecutor(max_workers=16) as pool:
        allowed = sum(decision.allowed for decision in pool.map(lambda _: limiter.allow(request), range(100)))
    assert allowed == 10
    now += 1_000_000_000
    assert sum(limiter.allow(request).allowed for _ in range(10)) == 5
    print("Python Rate Limiter: passed (atomic final-token race)")
