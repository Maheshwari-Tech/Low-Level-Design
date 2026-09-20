package com.example.lld.product_catalog_service.model;

import java.util.List;
import java.util.Objects;

public record ProductSchemaImpact(
        ProductId productId,
        long productVersion,
        ProductStatus status,
        List<ValidationIssue> before,
        List<ValidationIssue> after) {

    public ProductSchemaImpact {
        productId = Objects.requireNonNull(productId, "productId must not be null");
        if (productVersion < 1) {
            throw new IllegalArgumentException("productVersion must be at least one");
        }
        status = Objects.requireNonNull(status, "status must not be null");
        before = ModelSupport.immutableList(before, "before");
        after = ModelSupport.immutableList(after, "after");
    }

    public boolean newlyInvalid() {
        return before.isEmpty() && !after.isEmpty();
    }
}
