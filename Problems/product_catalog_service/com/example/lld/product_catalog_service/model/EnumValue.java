package com.example.lld.product_catalog_service.model;

public record EnumValue(String value) implements AttributeValue {
    public EnumValue {
        value = ModelSupport.requireNonBlank(value, "enum attribute value");
    }

    @Override
    public AttributeType type() {
        return AttributeType.ENUM;
    }

    @Override
    public String canonicalValue() {
        return value;
    }
}
