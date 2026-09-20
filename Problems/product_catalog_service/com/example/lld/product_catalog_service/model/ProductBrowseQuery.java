package com.example.lld.product_catalog_service.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProductBrowseQuery(
        Optional<CategoryId> categoryId,
        boolean includeDescendantCategories,
        Optional<String> brand,
        Map<AttributeKey, AttributeValue> productAttributes,
        Map<AttributeKey, AttributeValue> variantAttributes,
        Set<ProductStatus> statuses) {

    public ProductBrowseQuery {
        categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        brand = Objects.requireNonNull(brand, "brand must not be null")
                .map(value -> ModelSupport.requireNonBlank(value, "brand"));
        productAttributes = ModelSupport.immutableMap(productAttributes, "productAttributes");
        variantAttributes = ModelSupport.immutableMap(variantAttributes, "variantAttributes");
        Objects.requireNonNull(statuses, "statuses must not be null");
        LinkedHashSet<ProductStatus> statusCopy = new LinkedHashSet<>();
        for (ProductStatus status : statuses) {
            statusCopy.add(Objects.requireNonNull(status, "statuses contains a null value"));
        }
        statuses = Collections.unmodifiableSet(statusCopy);
    }

    public static ProductBrowseQuery activeCatalog() {
        return new ProductBrowseQuery(
                Optional.empty(),
                false,
                Optional.empty(),
                Map.of(),
                Map.of(),
                Set.of(ProductStatus.ACTIVE));
    }
}
