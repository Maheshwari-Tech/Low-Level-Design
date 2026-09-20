package com.example.lld.product_catalog_service.model;

public record CategoryId(String value) implements Comparable<CategoryId> {
    public CategoryId {
        value = ModelSupport.requireNonBlank(value, "category id");
    }

    @Override
    public int compareTo(CategoryId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
