package com.example.lld.product_catalog_service.model;

public record IntegerValue(long value) implements AttributeValue {
    @Override
    public AttributeType type() {
        return AttributeType.INTEGER;
    }

    @Override
    public String canonicalValue() {
        return Long.toString(value);
    }
}
