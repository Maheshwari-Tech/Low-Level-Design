package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.CategoryId;

public final class CategoryAlreadyExistsException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public CategoryAlreadyExistsException(CategoryId categoryId) {
        super("category already exists: " + categoryId);
    }
}
