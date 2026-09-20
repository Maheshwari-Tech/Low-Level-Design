package com.example.lld.product_catalog_service.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SchemaChangeImpactReport(
        CategoryId categoryId,
        long previousSchemaVersion,
        long newSchemaVersion,
        Instant changedAt,
        List<ProductSchemaImpact> affectedProducts) {

    public SchemaChangeImpactReport {
        categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        if (previousSchemaVersion < 1 || newSchemaVersion != previousSchemaVersion + 1) {
            throw new IllegalArgumentException("schema versions must represent one forward change");
        }
        changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        affectedProducts = ModelSupport.immutableList(affectedProducts, "affectedProducts");
    }
}
