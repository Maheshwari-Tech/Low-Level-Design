package com.example.lld.product_catalog_service.service;

import com.example.lld.product_catalog_service.model.AttributeDefinition;
import com.example.lld.product_catalog_service.model.AttributeKey;
import com.example.lld.product_catalog_service.model.AttributeValue;
import com.example.lld.product_catalog_service.model.CategorySchema;
import com.example.lld.product_catalog_service.model.Product;
import com.example.lld.product_catalog_service.model.Sku;
import com.example.lld.product_catalog_service.model.ValidationIssue;
import com.example.lld.product_catalog_service.model.Variant;
import com.example.lld.product_catalog_service.model.VariantId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class ProductValidator {
    List<ValidationIssue> validate(
            Product product,
            CategorySchema schema,
            boolean requirePublicationCompleteness) {
        List<ValidationIssue> issues = new ArrayList<>();

        validateValues("attributes", product.attributes(), schema.productAttributes(), issues);
        if (requirePublicationCompleteness) {
            if (product.description().isBlank()) {
                issues.add(new ValidationIssue("description", "is required for publication"));
            }
            addMissingRequired(
                    "attributes",
                    product.attributes(),
                    schema.productAttributes(),
                    issues);
            if (product.variants().isEmpty()) {
                issues.add(new ValidationIssue("variants", "at least one sellable variant is required"));
            }
        }

        Set<VariantId> variantIds = new HashSet<>();
        Set<Sku> skus = new HashSet<>();
        Set<Map<AttributeKey, AttributeValue>> combinations = new HashSet<>();
        for (int index = 0; index < product.variants().size(); index++) {
            Variant variant = product.variants().get(index);
            String path = "variants[" + index + "]";
            if (!variantIds.add(variant.id())) {
                issues.add(new ValidationIssue(path + ".id", "duplicate variant id " + variant.id()));
            }
            if (!skus.add(variant.sku())) {
                issues.add(new ValidationIssue(path + ".sku", "duplicate SKU " + variant.sku()));
            }
            validateValues(
                    path + ".attributes",
                    variant.attributes(),
                    schema.variantAttributes(),
                    issues);
            if (requirePublicationCompleteness) {
                addMissingRequired(
                        path + ".attributes",
                        variant.attributes(),
                        schema.variantAttributes(),
                        issues);
            }
            if (!schema.variantAttributes().isEmpty()) {
                Map<AttributeKey, AttributeValue> combination = canonicalCombination(variant, schema);
                if (!combinations.add(combination)) {
                    issues.add(new ValidationIssue(
                            path + ".attributes",
                            "duplicates another variant attribute combination"));
                }
            }
        }
        return List.copyOf(issues);
    }

    private static void validateValues(
            String path,
            Map<AttributeKey, AttributeValue> values,
            Map<AttributeKey, AttributeDefinition> definitions,
            List<ValidationIssue> issues) {
        values.forEach((key, value) -> {
            AttributeDefinition definition = definitions.get(key);
            if (definition == null) {
                issues.add(new ValidationIssue(path + "." + key, "is not defined by the category schema"));
                return;
            }
            definition.violation(value).ifPresent(message ->
                    issues.add(new ValidationIssue(path + "." + key, message)));
        });
    }

    private static void addMissingRequired(
            String path,
            Map<AttributeKey, AttributeValue> values,
            Map<AttributeKey, AttributeDefinition> definitions,
            List<ValidationIssue> issues) {
        definitions.values().stream()
                .filter(AttributeDefinition::required)
                .filter(definition -> !values.containsKey(definition.key()))
                .forEach(definition -> issues.add(new ValidationIssue(
                        path + "." + definition.key(),
                        "required attribute is missing")));
    }

    private static Map<AttributeKey, AttributeValue> canonicalCombination(
            Variant variant,
            CategorySchema schema) {
        Map<AttributeKey, AttributeValue> combination = new TreeMap<>();
        for (AttributeKey key : schema.variantAttributes().keySet()) {
            AttributeValue value = variant.attributes().get(key);
            if (value != null) {
                combination.put(key, value);
            }
        }
        return Collections.unmodifiableMap(combination);
    }
}
