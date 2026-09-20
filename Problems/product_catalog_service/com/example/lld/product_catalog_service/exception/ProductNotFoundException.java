package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;

public final class ProductNotFoundException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(ProductId productId) {
        super("product not found: " + productId);
    }
}
