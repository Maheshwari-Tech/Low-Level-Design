package com.example.lld.rate_limiter.time;

public final class SystemMonotonicClock implements MonotonicClock {
    @Override
    public long nanoTime() {
        return System.nanoTime();
    }
}
