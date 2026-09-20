from concurrent.futures import ThreadPoolExecutor
from threading import Barrier, Event
import unittest

from solution import (
    ManualClock,
    PolicyType,
    RateLimiter,
    RateLimitKey,
    RateLimitRequest,
    RateLimitRule,
)


SECOND = 1_000_000_000
KEY = RateLimitKey("tenant-a", "user-7", "/orders")


class RateLimiterTests(unittest.TestCase):
    def test_token_bucket_boundary_and_weighted_cost(self) -> None:
        clock = ManualClock()
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(
            RateLimitRule("checkout", 2, SECOND, PolicyType.TOKEN_BUCKET)
        )

        first = limiter.allow(RateLimitRequest("checkout", KEY, cost=2))
        denied = limiter.allow(RateLimitRequest("checkout", KEY))
        self.assertTrue(first.allowed)
        self.assertFalse(denied.allowed)
        self.assertEqual(denied.retry_after_ns, SECOND // 2)

        clock.advance(SECOND // 2)
        boundary = limiter.allow(RateLimitRequest("checkout", KEY))
        self.assertTrue(boundary.allowed)
        self.assertEqual(boundary.remaining, 0)

    def test_fixed_window_exact_reset(self) -> None:
        clock = ManualClock(100)
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(
            RateLimitRule("login", 1, 10, PolicyType.FIXED_WINDOW)
        )
        self.assertTrue(limiter.allow(RateLimitRequest("login", KEY)).allowed)
        self.assertFalse(limiter.allow(RateLimitRequest("login", KEY)).allowed)
        clock.advance(10)
        self.assertTrue(limiter.allow(RateLimitRequest("login", KEY)).allowed)

    def test_backward_clock_does_not_mint_refill_credit(self) -> None:
        now = [0]
        limiter = RateLimiter(lambda: now[0])
        limiter.register_rule(
            RateLimitRule("api", 1, 100, PolicyType.TOKEN_BUCKET)
        )
        request = RateLimitRequest("api", KEY)
        self.assertTrue(limiter.allow(request).allowed)

        now[0] = 50
        self.assertFalse(limiter.allow(request).allowed)
        now[0] = 0
        self.assertFalse(limiter.allow(request).allowed)
        now[0] = 50
        self.assertFalse(limiter.allow(request).allowed)

        now[0] = 100
        self.assertTrue(limiter.allow(request).allowed)

    def test_only_one_thread_wins_the_final_token(self) -> None:
        clock = ManualClock()
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(
            RateLimitRule("scarce", 1, SECOND, PolicyType.TOKEN_BUCKET)
        )
        barrier = Barrier(16)

        def compete(_: int) -> bool:
            barrier.wait()
            return limiter.allow(RateLimitRequest("scarce", KEY)).allowed

        with ThreadPoolExecutor(max_workers=16) as executor:
            outcomes = list(executor.map(compete, range(16)))
        self.assertEqual(sum(outcomes), 1)

    def test_rule_replacement_resets_a_cell_once(self) -> None:
        clock = ManualClock()
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(RateLimitRule("api", 1, 10, PolicyType.FIXED_WINDOW))
        self.assertTrue(limiter.allow(RateLimitRequest("api", KEY)).allowed)
        limiter.replace_rule(
            "api", 1, RateLimitRule("api", 2, 10, PolicyType.FIXED_WINDOW, version=2)
        )
        decision = limiter.allow(RateLimitRequest("api", KEY))
        self.assertTrue(decision.allowed)
        self.assertEqual(decision.rule_version, 2)
        self.assertEqual(decision.remaining, 1)

    def test_idle_eviction(self) -> None:
        clock = ManualClock()
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(RateLimitRule("api", 1, 10, PolicyType.FIXED_WINDOW))
        limiter.allow(RateLimitRequest("api", KEY))
        clock.advance(50)
        self.assertEqual(limiter.evict_idle(50), 1)

    def test_caller_retries_when_its_resolved_cell_was_evicted(self) -> None:
        clock = ManualClock()
        limiter = RateLimiter(clock.now_ns)
        limiter.register_rule(RateLimitRule("api", 1, 10, PolicyType.FIXED_WINDOW))
        request = RateLimitRequest("api", KEY)
        limiter.allow(request)  # exhaust the original cell

        entered = Event()
        released = Event()

        class PausingLock:
            def __enter__(self):
                entered.set()
                if not released.wait(timeout=1):
                    raise TimeoutError("test did not release the resolved cell")

            def __exit__(self, exc_type, exc_value, traceback):
                return False

        cell_key = ("api", KEY)
        orphan = limiter._cells[cell_key]
        orphan.lock = PausingLock()
        with ThreadPoolExecutor(max_workers=1) as executor:
            pending = executor.submit(limiter.allow, request)
            self.assertTrue(entered.wait(timeout=1))
            with limiter._registry_lock:
                del limiter._cells[cell_key]  # eviction won before cell-lock acquisition
            released.set()
            decision = pending.result(timeout=1)

        self.assertTrue(decision.allowed)
        self.assertIsNot(limiter._cells[cell_key], orphan)


if __name__ == "__main__":
    unittest.main(verbosity=2)
