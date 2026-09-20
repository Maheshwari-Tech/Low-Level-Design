package com.example.lld.rate_limiter.time;

import java.math.BigInteger;
import java.time.Duration;

/** Overflow-safe monotonic-time arithmetic shared by policies and cleanup. */
public final class TimeMath {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private TimeMath() {}

    public static long elapsed(long now, long before) {
        if (now <= before) {
            return 0;
        }
        try {
            return Math.subtractExact(now, before);
        } catch (ArithmeticException error) {
            return Long.MAX_VALUE;
        }
    }

    public static long saturatingAdd(long left, long right) {
        if (right < 0) {
            throw new IllegalArgumentException("right cannot be negative");
        }
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException error) {
            return Long.MAX_VALUE;
        }
    }

    public static long ceilDivideToLong(BigInteger numerator, BigInteger denominator) {
        if (numerator.signum() <= 0) {
            return 0;
        }
        BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(denominator);
        BigInteger result = quotientAndRemainder[1].signum() == 0
                ? quotientAndRemainder[0]
                : quotientAndRemainder[0].add(BigInteger.ONE);
        return result.compareTo(LONG_MAX) > 0 ? Long.MAX_VALUE : result.longValueExact();
    }

    public static Duration duration(long nanos) {
        return Duration.ofNanos(Math.max(0, nanos));
    }
}
