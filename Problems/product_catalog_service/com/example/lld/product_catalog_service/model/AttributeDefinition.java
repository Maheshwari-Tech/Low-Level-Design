package com.example.lld.product_catalog_service.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record AttributeDefinition(
        AttributeKey key,
        String displayName,
        AttributeType type,
        AttributeScope scope,
        boolean required,
        Set<String> allowedValues) {

    public AttributeDefinition {
        key = Objects.requireNonNull(key, "key must not be null");
        displayName = ModelSupport.requireNonBlank(displayName, "displayName");
        type = Objects.requireNonNull(type, "type must not be null");
        scope = Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(allowedValues, "allowedValues must not be null");

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String allowedValue : allowedValues) {
            values.add(ModelSupport.requireNonBlank(allowedValue, "allowed enum value"));
        }
        if (type == AttributeType.ENUM && values.isEmpty()) {
            throw new IllegalArgumentException("ENUM attribute " + key + " must define allowed values");
        }
        if (type != AttributeType.ENUM && !values.isEmpty()) {
            throw new IllegalArgumentException("allowed values are supported only for ENUM attributes");
        }
        allowedValues = Collections.unmodifiableSet(values);
    }

    public Optional<String> violation(AttributeValue value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.type() != type) {
            return Optional.of("expected " + type + " but received " + value.type());
        }
        if (type == AttributeType.ENUM && !allowedValues.contains(value.canonicalValue())) {
            return Optional.of(
                    "value '" + value.canonicalValue() + "' is not one of " + allowedValues);
        }
        return Optional.empty();
    }
}
