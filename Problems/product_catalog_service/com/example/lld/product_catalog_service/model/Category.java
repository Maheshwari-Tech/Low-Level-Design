package com.example.lld.product_catalog_service.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record Category(
        CategoryId id,
        String name,
        Optional<CategoryId> parentId,
        List<AttributeDefinition> localDefinitions,
        long schemaVersion,
        Instant updatedAt,
        String updatedBy) {

    public Category {
        id = Objects.requireNonNull(id, "id must not be null");
        name = ModelSupport.requireNonBlank(name, "name");
        parentId = Objects.requireNonNull(parentId, "parentId must not be null");
        localDefinitions = ModelSupport.immutableList(localDefinitions, "localDefinitions");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be at least one");
        }
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        updatedBy = ModelSupport.requireNonBlank(updatedBy, "updatedBy");
        if (parentId.isPresent() && parentId.orElseThrow().equals(id)) {
            throw new IllegalArgumentException("a category cannot be its own parent");
        }

        Set<String> definitionKeys = new HashSet<>();
        for (AttributeDefinition definition : localDefinitions) {
            String scopedKey = definition.scope() + ":" + definition.key();
            if (!definitionKeys.add(scopedKey)) {
                throw new IllegalArgumentException("duplicate local attribute definition " + scopedKey);
            }
        }
    }

    public Category changeSchema(
            List<AttributeDefinition> definitions,
            Instant changedAt,
            String actor) {
        return new Category(
                id,
                name,
                parentId,
                definitions,
                schemaVersion + 1,
                changedAt,
                actor);
    }
}
