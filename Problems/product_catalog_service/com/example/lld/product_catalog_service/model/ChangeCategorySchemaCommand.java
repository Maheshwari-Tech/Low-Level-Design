package com.example.lld.product_catalog_service.model;

import java.util.List;
import java.util.Objects;

public record ChangeCategorySchemaCommand(
        CategoryId categoryId,
        long expectedSchemaVersion,
        List<AttributeDefinition> localDefinitions,
        String actor) {

    public ChangeCategorySchemaCommand {
        categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        if (expectedSchemaVersion < 1) {
            throw new IllegalArgumentException("expectedSchemaVersion must be at least one");
        }
        localDefinitions = ModelSupport.immutableList(localDefinitions, "localDefinitions");
        actor = ModelSupport.requireNonBlank(actor, "actor");
    }
}
