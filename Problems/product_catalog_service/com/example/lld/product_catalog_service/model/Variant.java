package com.example.lld.product_catalog_service.model;

import java.util.Map;
import java.util.Objects;

public record Variant(
        VariantId id,
        Sku sku,
        Map<AttributeKey, AttributeValue> attributes) {

    public Variant {
        id = Objects.requireNonNull(id, "id must not be null");
        sku = Objects.requireNonNull(sku, "sku must not be null");
        attributes = ModelSupport.immutableMap(attributes, "variant attributes");
    }
}
