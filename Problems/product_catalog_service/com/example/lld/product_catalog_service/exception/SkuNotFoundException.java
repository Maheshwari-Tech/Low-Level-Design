package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.Sku;

public final class SkuNotFoundException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public SkuNotFoundException(Sku sku) {
        super("SKU not found: " + sku);
    }
}
