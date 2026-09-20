package com.example.lld.rate_limiter.policy;

import com.example.lld.rate_limiter.domain.PolicyType;
import com.example.lld.rate_limiter.domain.RateLimitRule;
import com.example.lld.rate_limiter.time.TimeMath;

/** Fixed window anchored to the first request; now == boundary starts the new window. */
public final class FixedWindowPolicy implements RateLimitPolicy {
    private static final class FixedWindowState implements PolicyState {
        private long windowStartNanos;
        private long used;

        private FixedWindowState(long windowStartNanos) {
            this.windowStartNanos = windowStartNanos;
        }
    }

    @Override
    public PolicyType type() {
        return PolicyType.FIXED_WINDOW;
    }

    @Override
    public PolicyState initialState(RateLimitRule rule, long nowNanos) {
        return new FixedWindowState(nowNanos);
    }

    @Override
    public PolicyResult evaluate(
            PolicyState state, RateLimitRule rule, long cost, long nowNanos) {
        if (!(state instanceof FixedWindowState window)) {
            throw new IllegalArgumentException("State does not belong to fixed window policy");
        }
        long period = rule.periodNanos();
        long elapsed = TimeMath.elapsed(nowNanos, window.windowStartNanos);
        if (elapsed >= period) {
            long completeWindows = elapsed / period;
            try {
                long advance = Math.multiplyExact(completeWindows, period);
                window.windowStartNanos = Math.addExact(window.windowStartNanos, advance);
            } catch (ArithmeticException overflow) {
                window.windowStartNanos = nowNanos;
            }
            window.used = 0;
        }

        long available = rule.limit() - window.used;
        boolean allowed = cost <= available;
        if (allowed) {
            window.used += cost;
        }
        long remaining = rule.limit() - window.used;
        long resetAt = TimeMath.saturatingAdd(window.windowStartNanos, period);
        long resetNanos = resetAt == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : TimeMath.elapsed(resetAt, nowNanos);
        return new PolicyResult(
                allowed,
                remaining,
                resetAt,
                TimeMath.duration(resetNanos),
                allowed ? TimeMath.duration(0) : TimeMath.duration(resetNanos));
    }
}
