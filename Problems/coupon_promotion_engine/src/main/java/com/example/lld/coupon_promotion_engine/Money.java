package com.example.lld.coupon_promotion_engine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/** An exact, non-negative monetary amount represented in the currency's minor unit. */
public record Money(String currency, long minorUnits) implements Comparable<Money> {
    private static final BigInteger TEN_THOUSAND = BigInteger.valueOf(10_000L);

    public Money {
        Objects.requireNonNull(currency, "currency");
        currency = currency.toUpperCase(Locale.ROOT);
        Currency.getInstance(currency);
        if (minorUnits < 0L) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public static Money zero(String currency) {
        return new Money(currency, 0L);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(currency, Math.addExact(minorUnits, other.minorUnits));
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        if (other.minorUnits > minorUnits) {
            throw new IllegalArgumentException("Subtraction would make money negative");
        }
        return new Money(currency, minorUnits - other.minorUnits);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        return new Money(currency, Math.multiplyExact(minorUnits, quantity));
    }

    /** Applies basis points using half-up rounding; 1% is 100 basis points. */
    public Money percentage(int basisPoints) {
        if (basisPoints < 0) {
            throw new IllegalArgumentException("Basis points cannot be negative");
        }
        BigInteger numerator = BigInteger.valueOf(minorUnits)
                .multiply(BigInteger.valueOf(basisPoints));
        BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(TEN_THOUSAND);
        BigInteger rounded = quotientAndRemainder[0];
        if (quotientAndRemainder[1].shiftLeft(1).compareTo(TEN_THOUSAND) >= 0) {
            rounded = rounded.add(BigInteger.ONE);
        }
        return new Money(currency, rounded.longValueExact());
    }

    public Money min(Money other) {
        requireSameCurrency(other);
        return minorUnits <= other.minorUnits ? this : other;
    }

    public boolean isZero() {
        return minorUnits == 0L;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " and " + other.currency);
        }
    }

    @Override
    public String toString() {
        int scale = Currency.getInstance(currency).getDefaultFractionDigits();
        return currency + " " + BigDecimal.valueOf(minorUnits, scale).toPlainString();
    }
}
