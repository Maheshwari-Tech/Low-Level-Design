package com.example.lld.product_catalog_service.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record UpdateProductCommand(
        ProductId productId,
        long expectedVersion,
        String name,
        String description,
        String brand,
        CategoryId categoryId,
        Map<AttributeKey, AttributeValue> attributes,
        List<Variant> variants,
        List<MediaAsset> media,
        String actor) {

    public UpdateProductCommand {
        productId = Objects.requireNonNull(productId, "productId must not be null");
        if (expectedVersion < 1) {
            throw new IllegalArgumentException("expectedVersion must be at least one");
        }
        name = ModelSupport.requireNonBlank(name, "name");
        description = ModelSupport.trimToEmpty(description, "description");
        brand = ModelSupport.requireNonBlank(brand, "brand");
        categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        attributes = ModelSupport.immutableMap(attributes, "attributes");
        variants = ModelSupport.immutableList(variants, "variants");
        media = ModelSupport.immutableList(media, "media");
        actor = ModelSupport.requireNonBlank(actor, "actor");
    }
}
