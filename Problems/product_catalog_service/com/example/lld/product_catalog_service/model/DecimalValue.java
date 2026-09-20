package com.example.lld.product_catalog_service.model;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalValue(BigDecimal value) implements AttributeValue {
    public DecimalValue {
        value = Objects.requireNonNull(value, "decimal attribute value must not be null").stripTrailingZeros();
    }

    @Override
    public AttributeType type() {
        return AttributeType.DECIMAL;
    }

    @Override
    public String canonicalValue() {
        return value.toPlainString();
    }
}
