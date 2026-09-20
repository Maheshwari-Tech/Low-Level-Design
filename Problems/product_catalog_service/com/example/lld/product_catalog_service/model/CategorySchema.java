package com.example.lld.product_catalog_service.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CategorySchema(
        Map<AttributeKey, AttributeDefinition> productAttributes,
        Map<AttributeKey, AttributeDefinition> variantAttributes) {

    public CategorySchema {
        productAttributes = ModelSupport.immutableMap(productAttributes, "productAttributes");
        variantAttributes = ModelSupport.immutableMap(variantAttributes, "variantAttributes");
        verifyScopes(productAttributes, AttributeScope.PRODUCT);
        verifyScopes(variantAttributes, AttributeScope.VARIANT);
    }

    public static CategorySchema empty() {
        return new CategorySchema(Map.of(), Map.of());
    }

    public CategorySchema overlay(List<AttributeDefinition> localDefinitions) {
        Map<AttributeKey, AttributeDefinition> products = new LinkedHashMap<>(productAttributes);
        Map<AttributeKey, AttributeDefinition> variants = new LinkedHashMap<>(variantAttributes);
        for (AttributeDefinition definition : localDefinitions) {
            Map<AttributeKey, AttributeDefinition> target =
                    definition.scope() == AttributeScope.PRODUCT ? products : variants;
            target.put(definition.key(), definition);
        }
        return new CategorySchema(products, variants);
    }

    private static void verifyScopes(
            Map<AttributeKey, AttributeDefinition> definitions,
            AttributeScope expectedScope) {
        definitions.forEach((key, definition) -> {
            if (!key.equals(definition.key()) || definition.scope() != expectedScope) {
                throw new IllegalArgumentException(
                        "schema entry " + key + " does not match its definition or scope");
            }
        });
    }
}
