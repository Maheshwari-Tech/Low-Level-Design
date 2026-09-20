package com.example.lld.product_catalog_service.model;

public record ProductId(String value) implements Comparable<ProductId> {
    public ProductId {
        value = ModelSupport.requireNonBlank(value, "product id");
    }

    @Override
    public int compareTo(ProductId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
