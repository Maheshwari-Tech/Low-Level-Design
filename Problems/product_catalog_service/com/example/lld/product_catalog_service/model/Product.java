package com.example.lld.product_catalog_service.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Product(
        ProductId id,
        String name,
        String description,
        String brand,
        CategoryId categoryId,
        Map<AttributeKey, AttributeValue> attributes,
        List<Variant> variants,
        List<MediaAsset> media,
        ProductStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy) {

    public Product {
        id = Objects.requireNonNull(id, "id must not be null");
        name = ModelSupport.requireNonBlank(name, "name");
        description = ModelSupport.trimToEmpty(description, "description");
        brand = ModelSupport.requireNonBlank(brand, "brand");
        categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        attributes = ModelSupport.immutableMap(attributes, "product attributes");
        variants = ModelSupport.immutableList(variants, "variants");
        media = ModelSupport.immutableList(media, "media");
        status = Objects.requireNonNull(status, "status must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least one");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        updatedBy = ModelSupport.requireNonBlank(updatedBy, "updatedBy");
    }

    public Product revise(
            String revisedName,
            String revisedDescription,
            String revisedBrand,
            CategoryId revisedCategoryId,
            Map<AttributeKey, AttributeValue> revisedAttributes,
            List<Variant> revisedVariants,
            List<MediaAsset> revisedMedia,
            Instant revisedAt,
            String actor) {
        return new Product(
                id,
                revisedName,
                revisedDescription,
                revisedBrand,
                revisedCategoryId,
                revisedAttributes,
                revisedVariants,
                revisedMedia,
                status,
                version + 1,
                createdAt,
                revisedAt,
                actor);
    }

    public Product transitionTo(ProductStatus newStatus, Instant changedAt, String actor) {
        return new Product(
                id,
                name,
                description,
                brand,
                categoryId,
                attributes,
                variants,
                media,
                newStatus,
                version + 1,
                createdAt,
                changedAt,
                actor);
    }
}
