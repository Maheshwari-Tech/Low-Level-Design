package com.example.lld.product_catalog_service.model;

public sealed interface AttributeValue
        permits TextValue, IntegerValue, DecimalValue, BooleanValue, EnumValue {
    AttributeType type();

    String canonicalValue();
}
