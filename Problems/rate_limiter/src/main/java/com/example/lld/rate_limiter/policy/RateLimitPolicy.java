package com.example.lld.rate_limiter.policy;

import com.example.lld.rate_limiter.domain.PolicyType;
import com.example.lld.rate_limiter.domain.RateLimitRule;

/** Strategy port; implementations mutate only the supplied cell-owned state. */
public interface RateLimitPolicy {
    PolicyType type();

    PolicyState initialState(RateLimitRule rule, long nowNanos);

    PolicyResult evaluate(PolicyState state, RateLimitRule rule, long cost, long nowNanos);
}
