package com.example.lld.rate_limiter.time;

/** A time source whose values are meaningful only for elapsed-time calculation. */
@FunctionalInterface
public interface MonotonicClock {
    long nanoTime();
}
