package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ProductStatus;

public final class InvalidProductStateException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public InvalidProductStateException(
            ProductId productId,
            ProductStatus current,
            ProductStatus requested) {
        super("product " + productId + " cannot transition from " + current + " to " + requested);
    }

    public InvalidProductStateException(ProductId productId, String operation, ProductStatus current) {
        super("product " + productId + " cannot be " + operation + " while it is " + current);
    }
}
