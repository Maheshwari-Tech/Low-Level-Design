package com.example.lld.rate_limiter.time;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class ManualMonotonicClock implements MonotonicClock {
    private final AtomicLong nowNanos;

    public ManualMonotonicClock() {
        this(0);
    }

    public ManualMonotonicClock(long initialNanos) {
        if (initialNanos < 0) {
            throw new IllegalArgumentException("initialNanos cannot be negative");
        }
        nowNanos = new AtomicLong(initialNanos);
    }

    @Override
    public long nanoTime() {
        return nowNanos.get();
    }

    public long advance(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Manual clock cannot move backwards");
        }
        long delta;
        try {
            delta = duration.toNanos();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Duration is too large", error);
        }
        return nowNanos.updateAndGet(current -> {
            try {
                return Math.addExact(current, delta);
            } catch (ArithmeticException error) {
                return Long.MAX_VALUE;
            }
        });
    }
}
