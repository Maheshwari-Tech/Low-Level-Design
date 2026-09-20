package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.CategoryId;

public final class CategoryNotFoundException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public CategoryNotFoundException(CategoryId categoryId) {
        super("category not found: " + categoryId);
    }
}
