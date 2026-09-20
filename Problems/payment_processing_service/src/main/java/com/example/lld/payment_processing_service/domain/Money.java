package com.example.lld.payment_processing_service.domain;

import com.example.lld.payment_processing_service.exception.InvalidMoneyException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Exact, immutable money. Floating-point construction is intentionally unsupported. */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        int scale = currency.getDefaultFractionDigits();
        if (scale < 0) {
            throw new InvalidMoneyException(
                    "Currency has no supported fixed fraction digits: " + currency);
        }
        try {
            amount = amount.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new InvalidMoneyException(
                    "Amount has more fraction digits than " + currency.getCurrencyCode()
                            + " supports: " + amount,
                    error);
        }
        if (amount.signum() < 0) {
            throw new InvalidMoneyException("Money cannot be negative: " + amount);
        }
    }

    public static Money of(String amount, String currencyCode) {
        try {
            return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
        } catch (InvalidMoneyException error) {
            throw error;
        } catch (IllegalArgumentException error) {
            throw new InvalidMoneyException(
                    "Invalid money value " + amount + " " + currencyCode, error);
        }
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        if (amount.compareTo(other.amount) < 0) {
            throw new InvalidMoneyException("Money subtraction would be negative");
        }
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new InvalidMoneyException(
                    "Currency mismatch: " + currency + " and " + other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
