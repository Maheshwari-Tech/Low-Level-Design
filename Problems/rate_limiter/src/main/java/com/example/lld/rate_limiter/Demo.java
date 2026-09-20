package com.example.lld.rate_limiter;

import com.example.lld.rate_limiter.domain.PolicyType;
import com.example.lld.rate_limiter.domain.RateLimitDecision;
import com.example.lld.rate_limiter.domain.RateLimitKey;
import com.example.lld.rate_limiter.domain.RateLimitRequest;
import com.example.lld.rate_limiter.domain.RateLimitRule;
import com.example.lld.rate_limiter.exception.InvalidRateLimitRuleException;
import com.example.lld.rate_limiter.service.RateLimiter;
import com.example.lld.rate_limiter.time.ManualMonotonicClock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runnable deterministic checks, including a real concurrent final-token race. */
public final class Demo {
    private Demo() {}

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ManualMonotonicClock clock = new ManualMonotonicClock();
        RateLimiter limiter = RateLimiter.withStandardPolicies(clock);

        tokenBucketExhaustionAndRefillBoundary(limiter, clock);
        independentKeys(limiter);
        fixedWindowBoundary(limiter, clock);
        concurrentFinalTokenRace(limiter);
        runtimeRuleReset(limiter);
        idleEviction();
        invalidConfiguration();

        System.out.println("All rate limiter scenarios passed.");
    }

    private static void tokenBucketExhaustionAndRefillBoundary(
            RateLimiter limiter, ManualMonotonicClock clock) {
        limiter.registerRule(new RateLimitRule(
                "api-token", 2, Duration.ofSeconds(10), 0, PolicyType.TOKEN_BUCKET));
        RateLimitKey alice = RateLimitKey.of("user", "alice");
        check(one(limiter, "api-token", alice).allowed(), "first token rejected");
        check(one(limiter, "api-token", alice).allowed(), "second token rejected");
        RateLimitDecision denied = one(limiter, "api-token", alice);
        check(!denied.allowed(), "empty bucket allowed request");
        check(denied.remaining() == 0, "remaining became negative");
        check(denied.retryAfter().equals(Duration.ofSeconds(5)), "wrong retry boundary");

        clock.advance(Duration.ofSeconds(5));
        RateLimitDecision exactBoundary = one(limiter, "api-token", alice);
        check(exactBoundary.allowed(), "request at exact refill boundary was rejected");
        System.out.println("token bucket exhaustion: retry-after=5s; exact refill boundary allowed");
    }

    private static void independentKeys(RateLimiter limiter) {
        RateLimitDecision bob = one(limiter, "api-token", RateLimitKey.of("user", "bob"));
        check(bob.allowed() && bob.remaining() == 1, "unrelated key shared state");
        System.out.println("independent key state: verified");
    }

    private static void fixedWindowBoundary(
            RateLimiter limiter, ManualMonotonicClock clock) {
        limiter.registerRule(new RateLimitRule(
                "login-window", 2, Duration.ofSeconds(10), 0, PolicyType.FIXED_WINDOW));
        RateLimitKey ip = RateLimitKey.of("ip", "203.0.113.7");
        check(one(limiter, "login-window", ip).allowed(), "fixed window request 1 failed");
        check(one(limiter, "login-window", ip).allowed(), "fixed window request 2 failed");
        check(!one(limiter, "login-window", ip).allowed(), "fixed window exceeded limit");
        clock.advance(Duration.ofSeconds(10));
        check(one(limiter, "login-window", ip).allowed(), "exact window boundary did not reset");
        System.out.println("fixed window: exact reset boundary starts the new window");
    }

    private static void concurrentFinalTokenRace(RateLimiter limiter)
            throws InterruptedException, ExecutionException {
        limiter.registerRule(new RateLimitRule(
                "race", 2, Duration.ofHours(1), 0, PolicyType.TOKEN_BUCKET));
        RateLimitKey key = RateLimitKey.of("tenant", "race-tenant");
        check(one(limiter, "race", key).allowed(), "could not reserve first race token");

        int callers = 16;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < callers; index++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return one(limiter, "race", key).allowed();
                }));
            }
            check(ready.await(5, TimeUnit.SECONDS), "race workers did not become ready");
            start.countDown();
            int allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    allowed++;
                }
            }
            check(allowed == 1, "expected exactly one final-token winner, got " + allowed);
        } finally {
            start.countDown();
            pool.shutdown();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "race pool did not terminate");
        }
        System.out.println("concurrent final-token race: exactly 1 of 16 allowed");
    }

    private static void runtimeRuleReset(RateLimiter limiter) {
        RateLimitKey key = RateLimitKey.of("api-key", "runtime-client");
        limiter.registerRule(new RateLimitRule(
                "runtime", 1, Duration.ofMinutes(1), 0, PolicyType.TOKEN_BUCKET));
        check(one(limiter, "runtime", key).allowed(), "runtime initial request failed");
        check(!one(limiter, "runtime", key).allowed(), "runtime rule was not exhausted");
        long version = limiter.updateRule(new RateLimitRule(
                "runtime", 3, Duration.ofMinutes(1), 0, PolicyType.TOKEN_BUCKET));
        RateLimitDecision afterUpdate = one(limiter, "runtime", key);
        check(version == 2 && afterUpdate.ruleVersion() == 2, "rule version did not advance");
        check(afterUpdate.allowed() && afterUpdate.remaining() == 2, "updated rule did not reset");
        System.out.println("runtime update: version 2 lazily reset existing key state");
    }

    private static void idleEviction() {
        ManualMonotonicClock clock = new ManualMonotonicClock();
        RateLimiter limiter = RateLimiter.withStandardPolicies(clock);
        limiter.registerRule(new RateLimitRule(
                "eviction", 100, Duration.ofSeconds(1), 0, PolicyType.TOKEN_BUCKET));
        RateLimitKey idle = RateLimitKey.of("user", "idle");
        RateLimitKey active = RateLimitKey.of("user", "active");
        one(limiter, "eviction", idle);
        one(limiter, "eviction", active);
        clock.advance(Duration.ofSeconds(30));
        one(limiter, "eviction", active);
        clock.advance(Duration.ofSeconds(40));
        int evicted = limiter.evictIdle(Duration.ofSeconds(60));
        check(evicted == 1 && limiter.trackedKeyCount() == 1, "idle cleanup removed wrong cells");
        check(one(limiter, "eviction", active).allowed(), "cleanup changed active-key behaviour");
        System.out.println("idle eviction: idle removed, active retained");
    }

    private static void invalidConfiguration() {
        boolean rejected = false;
        try {
            new RateLimitRule(
                    "invalid", 0, Duration.ofSeconds(1), 0, PolicyType.TOKEN_BUCKET);
        } catch (InvalidRateLimitRuleException expected) {
            rejected = true;
        }
        check(rejected, "zero capacity configuration was accepted");
        System.out.println("invalid configuration: rejected");
    }

    private static RateLimitDecision one(
            RateLimiter limiter, String ruleId, RateLimitKey key) {
        return limiter.decide(RateLimitRequest.one(ruleId, key));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
