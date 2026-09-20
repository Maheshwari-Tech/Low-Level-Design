package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;

public final class ProductAlreadyExistsException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public ProductAlreadyExistsException(ProductId productId) {
        super("product already exists: " + productId);
    }
}
