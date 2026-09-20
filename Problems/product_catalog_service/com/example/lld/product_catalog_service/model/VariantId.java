package com.example.lld.product_catalog_service.model;

public record VariantId(String value) implements Comparable<VariantId> {
    public VariantId {
        value = ModelSupport.requireNonBlank(value, "variant id");
    }

    @Override
    public int compareTo(VariantId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
