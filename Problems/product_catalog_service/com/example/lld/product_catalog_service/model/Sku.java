package com.example.lld.product_catalog_service.model;

import java.util.Locale;

public record Sku(String value) implements Comparable<Sku> {
    public Sku {
        value = ModelSupport.requireNonBlank(value, "SKU").toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9][A-Z0-9._-]{0,63}")) {
            throw new IllegalArgumentException(
                    "SKU must contain only letters, digits, dots, underscores, or hyphens");
        }
    }

    @Override
    public int compareTo(Sku other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
