package com.example.lld.rate_limiter.policy;

import com.example.lld.rate_limiter.domain.PolicyType;
import com.example.lld.rate_limiter.domain.RateLimitRule;
import com.example.lld.rate_limiter.time.TimeMath;
import java.math.BigInteger;

/** Token bucket using exact integer token-nanosecond credits (no floating-point drift). */
public final class TokenBucketPolicy implements RateLimitPolicy {
    private static final class TokenBucketState implements PolicyState {
        private BigInteger credits;
        private long lastRefillNanos;

        private TokenBucketState(BigInteger credits, long lastRefillNanos) {
            this.credits = credits;
            this.lastRefillNanos = lastRefillNanos;
        }
    }

    @Override
    public PolicyType type() {
        return PolicyType.TOKEN_BUCKET;
    }

    @Override
    public PolicyState initialState(RateLimitRule rule, long nowNanos) {
        return new TokenBucketState(maxCredits(rule), nowNanos);
    }

    @Override
    public PolicyResult evaluate(
            PolicyState state, RateLimitRule rule, long cost, long nowNanos) {
        if (!(state instanceof TokenBucketState bucket)) {
            throw new IllegalArgumentException("State does not belong to token bucket policy");
        }
        BigInteger period = BigInteger.valueOf(rule.periodNanos());
        BigInteger capacity = BigInteger.valueOf(rule.capacity());
        BigInteger maximum = maxCredits(rule);
        long elapsedNanos = TimeMath.elapsed(nowNanos, bucket.lastRefillNanos);
        if (elapsedNanos > 0) {
            BigInteger refill = BigInteger.valueOf(elapsedNanos).multiply(capacity);
            bucket.credits = bucket.credits.add(refill).min(maximum);
            bucket.lastRefillNanos = nowNanos;
        }

        BigInteger required = BigInteger.valueOf(cost).multiply(period);
        boolean allowed = bucket.credits.compareTo(required) >= 0;
        if (allowed) {
            bucket.credits = bucket.credits.subtract(required);
        }
        long remaining = bucket.credits.divide(period).longValueExact();
        BigInteger refillNeeded = maximum.subtract(bucket.credits);
        long resetNanos = TimeMath.ceilDivideToLong(refillNeeded, capacity);
        long retryNanos = allowed
                ? 0
                : TimeMath.ceilDivideToLong(required.subtract(bucket.credits), capacity);
        return new PolicyResult(
                allowed,
                remaining,
                TimeMath.saturatingAdd(nowNanos, resetNanos),
                TimeMath.duration(resetNanos),
                TimeMath.duration(retryNanos));
    }

    private static BigInteger maxCredits(RateLimitRule rule) {
        return BigInteger.valueOf(rule.limit())
                .multiply(BigInteger.valueOf(rule.periodNanos()));
    }
}
