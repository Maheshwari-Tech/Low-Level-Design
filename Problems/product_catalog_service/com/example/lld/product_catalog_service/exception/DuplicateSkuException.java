package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.Sku;

public final class DuplicateSkuException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public DuplicateSkuException(Sku sku, ProductId owner) {
        super("SKU " + sku + " is already permanently owned by product " + owner);
    }
}
