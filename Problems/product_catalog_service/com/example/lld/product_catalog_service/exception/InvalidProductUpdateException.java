package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;

public final class InvalidProductUpdateException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public InvalidProductUpdateException(ProductId productId, String reason) {
        super("invalid update for product " + productId + ": " + reason);
    }
}
