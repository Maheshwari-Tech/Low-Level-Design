package com.example.lld.product_catalog_service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ModelSupport {
    private ModelSupport() {
    }

    static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    static String trimToEmpty(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        return value.trim();
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> source, String fieldName) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        Map<K, V> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, fieldName + " contains a null key"),
                Objects.requireNonNull(value, fieldName + " contains a null value")));
        return Collections.unmodifiableMap(copy);
    }

    static <T> List<T> immutableList(List<T> source, String fieldName) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        List<T> copy = new ArrayList<>(source.size());
        for (T value : source) {
            copy.add(Objects.requireNonNull(value, fieldName + " contains a null value"));
        }
        return Collections.unmodifiableList(copy);
    }
}
