package com.example.lld.product_catalog_service.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreateCategoryCommand(
        CategoryId id,
        String name,
        Optional<CategoryId> parentId,
        List<AttributeDefinition> localDefinitions,
        String actor) {

    public CreateCategoryCommand {
        id = Objects.requireNonNull(id, "id must not be null");
        name = ModelSupport.requireNonBlank(name, "name");
        parentId = Objects.requireNonNull(parentId, "parentId must not be null");
        localDefinitions = ModelSupport.immutableList(localDefinitions, "localDefinitions");
        actor = ModelSupport.requireNonBlank(actor, "actor");
    }
}
