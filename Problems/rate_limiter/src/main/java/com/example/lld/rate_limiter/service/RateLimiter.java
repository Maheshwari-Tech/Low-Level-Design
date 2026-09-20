package com.example.lld.rate_limiter.service;

import com.example.lld.rate_limiter.domain.PolicyType;
import com.example.lld.rate_limiter.domain.RateLimitDecision;
import com.example.lld.rate_limiter.domain.RateLimitKey;
import com.example.lld.rate_limiter.domain.RateLimitRequest;
import com.example.lld.rate_limiter.domain.RateLimitRule;
import com.example.lld.rate_limiter.exception.DuplicateRateLimitRuleException;
import com.example.lld.rate_limiter.exception.InvalidRateLimitRequestException;
import com.example.lld.rate_limiter.exception.UnknownRateLimitRuleException;
import com.example.lld.rate_limiter.policy.FixedWindowPolicy;
import com.example.lld.rate_limiter.policy.PolicyResult;
import com.example.lld.rate_limiter.policy.PolicyState;
import com.example.lld.rate_limiter.policy.RateLimitPolicy;
import com.example.lld.rate_limiter.policy.TokenBucketPolicy;
import com.example.lld.rate_limiter.time.MonotonicClock;
import com.example.lld.rate_limiter.time.TimeMath;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory rate limiter. Decisions serialize only callers sharing a rule and key.
 * Rule updates reset state lazily at each key's next decision.
 */
public final class RateLimiter {
    private record StateKey(String ruleId, RateLimitKey key) {}

    private record VersionedRule(RateLimitRule rule, long version) {}

    private static final class StateCell {
        private final ReentrantLock lock = new ReentrantLock();
        private PolicyState state;
        private long ruleVersion;
        private PolicyType policyType;
        private volatile long lastAccessNanos;

        private StateCell(
                PolicyState state,
                long ruleVersion,
                PolicyType policyType,
                long lastAccessNanos) {
            this.state = state;
            this.ruleVersion = ruleVersion;
            this.policyType = policyType;
            this.lastAccessNanos = lastAccessNanos;
        }
    }

    private final MonotonicClock clock;
    private final Map<PolicyType, RateLimitPolicy> policies;
    private final ConcurrentHashMap<String, VersionedRule> rules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<StateKey, StateCell> states = new ConcurrentHashMap<>();

    public RateLimiter(
            MonotonicClock clock, Collection<? extends RateLimitPolicy> policyImplementations) {
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(policyImplementations, "policyImplementations");
        Map<PolicyType, RateLimitPolicy> configured = new LinkedHashMap<>();
        for (RateLimitPolicy policy : policyImplementations) {
            Objects.requireNonNull(policy, "policy");
            if (configured.putIfAbsent(policy.type(), policy) != null) {
                throw new IllegalArgumentException("Duplicate policy: " + policy.type());
            }
        }
        if (configured.isEmpty()) {
            throw new IllegalArgumentException("At least one policy is required");
        }
        policies = Map.copyOf(configured);
    }

    public static RateLimiter withStandardPolicies(MonotonicClock clock) {
        return new RateLimiter(clock, List.of(new TokenBucketPolicy(), new FixedWindowPolicy()));
    }

    public void registerRule(RateLimitRule rule) {
        Objects.requireNonNull(rule, "rule");
        requirePolicy(rule.policyType());
        if (rules.putIfAbsent(rule.ruleId(), new VersionedRule(rule, 1)) != null) {
            throw new DuplicateRateLimitRuleException(rule.ruleId());
        }
    }

    /** Replaces a rule and resets every existing key lazily on its next decision. */
    public long updateRule(RateLimitRule rule) {
        Objects.requireNonNull(rule, "rule");
        requirePolicy(rule.policyType());
        VersionedRule updated = rules.compute(rule.ruleId(), (ruleId, current) -> {
            if (current == null) {
                throw new UnknownRateLimitRuleException(ruleId);
            }
            long nextVersion;
            try {
                nextVersion = Math.incrementExact(current.version());
            } catch (ArithmeticException error) {
                throw new IllegalStateException("Rule version overflow: " + ruleId, error);
            }
            return new VersionedRule(rule, nextVersion);
        });
        return updated.version();
    }

    public RateLimitDecision decide(RateLimitRequest request) {
        Objects.requireNonNull(request, "request");
        StateKey stateKey = new StateKey(request.ruleId(), request.key());
        while (true) {
            VersionedRule observedRule = requireRule(request.ruleId());
            long observedNow = clock.nanoTime();
            StateCell cell = states.computeIfAbsent(
                    stateKey, ignored -> newCell(observedRule, observedNow));
            cell.lock.lock();
            try {
                // An eviction can remove a cell after lookup but before lock acquisition.
                if (states.get(stateKey) != cell) {
                    continue;
                }
                VersionedRule currentRule = requireRule(request.ruleId());
                RateLimitRule rule = currentRule.rule();
                if (request.cost() > rule.limit()) {
                    throw new InvalidRateLimitRequestException(
                            "cost " + request.cost() + " exceeds rule limit " + rule.limit());
                }
                long now = clock.nanoTime();
                if (cell.ruleVersion != currentRule.version()
                        || cell.policyType != rule.policyType()) {
                    RateLimitPolicy currentPolicy = requirePolicy(rule.policyType());
                    cell.state = currentPolicy.initialState(rule, now);
                    cell.ruleVersion = currentRule.version();
                    cell.policyType = rule.policyType();
                }

                RateLimitPolicy policy = requirePolicy(rule.policyType());
                PolicyResult policyResult =
                        policy.evaluate(cell.state, rule, request.cost(), now);
                cell.lastAccessNanos = now;
                return new RateLimitDecision(
                        policyResult.allowed(),
                        rule.ruleId(),
                        request.key(),
                        rule.policyType(),
                        rule.limit(),
                        policyResult.remaining(),
                        policyResult.resetAtNanos(),
                        policyResult.resetAfter(),
                        policyResult.retryAfter(),
                        currentRule.version());
            } finally {
                cell.lock.unlock();
            }
        }
    }

    /**
     * Removes state whose last completed decision is at least idleFor old. A cell is
     * identity-checked under its lock, so cleanup cannot race a committed decision.
     */
    public int evictIdle(Duration idleFor) {
        Objects.requireNonNull(idleFor, "idleFor");
        if (idleFor.isNegative()) {
            throw new IllegalArgumentException("idleFor cannot be negative");
        }
        long idleNanos;
        try {
            idleNanos = idleFor.toNanos();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("idleFor is too large", error);
        }
        long now = clock.nanoTime();
        AtomicInteger removed = new AtomicInteger();
        states.forEach((key, cell) -> {
            if (TimeMath.elapsed(now, cell.lastAccessNanos) < idleNanos) {
                return;
            }
            if (!cell.lock.tryLock()) {
                return;
            }
            try {
                if (states.get(key) == cell
                        && TimeMath.elapsed(now, cell.lastAccessNanos) >= idleNanos
                        && states.remove(key, cell)) {
                    removed.incrementAndGet();
                }
            } finally {
                cell.lock.unlock();
            }
        });
        return removed.get();
    }

    public int trackedKeyCount() {
        return states.size();
    }

    public long ruleVersion(String ruleId) {
        return requireRule(ruleId).version();
    }

    private StateCell newCell(VersionedRule versionedRule, long now) {
        RateLimitRule rule = versionedRule.rule();
        RateLimitPolicy policy = requirePolicy(rule.policyType());
        return new StateCell(
                policy.initialState(rule, now),
                versionedRule.version(),
                rule.policyType(),
                now);
    }

    private VersionedRule requireRule(String ruleId) {
        VersionedRule rule = rules.get(ruleId);
        if (rule == null) {
            throw new UnknownRateLimitRuleException(ruleId);
        }
        return rule;
    }

    private RateLimitPolicy requirePolicy(PolicyType policyType) {
        RateLimitPolicy policy = policies.get(policyType);
        if (policy == null) {
            throw new IllegalArgumentException("No implementation for policy: " + policyType);
        }
        return policy;
    }
}
