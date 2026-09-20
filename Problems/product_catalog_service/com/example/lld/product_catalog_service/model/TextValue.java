package com.example.lld.product_catalog_service.model;

public record TextValue(String value) implements AttributeValue {
    public TextValue {
        value = ModelSupport.requireNonBlank(value, "text attribute value");
    }

    @Override
    public AttributeType type() {
        return AttributeType.TEXT;
    }

    @Override
    public String canonicalValue() {
        return value;
    }
}
