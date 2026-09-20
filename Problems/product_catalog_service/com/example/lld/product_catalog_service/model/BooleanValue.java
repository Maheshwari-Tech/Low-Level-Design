package com.example.lld.product_catalog_service.model;

public record BooleanValue(boolean value) implements AttributeValue {
    @Override
    public AttributeType type() {
        return AttributeType.BOOLEAN;
    }

    @Override
    public String canonicalValue() {
        return Boolean.toString(value);
    }
}
