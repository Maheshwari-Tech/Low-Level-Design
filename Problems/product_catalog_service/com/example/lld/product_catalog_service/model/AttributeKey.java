package com.example.lld.product_catalog_service.model;

import java.util.Locale;

public record AttributeKey(String value) implements Comparable<AttributeKey> {
    public AttributeKey {
        value = ModelSupport.requireNonBlank(value, "attribute key").toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9_]{0,63}")) {
            throw new IllegalArgumentException(
                    "attribute key must start with a letter and contain only lowercase letters, digits, or underscores");
        }
    }

    @Override
    public int compareTo(AttributeKey other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
