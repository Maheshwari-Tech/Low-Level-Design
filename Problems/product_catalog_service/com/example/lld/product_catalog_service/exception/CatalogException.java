package com.example.lld.product_catalog_service.exception;

public class CatalogException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CatalogException(String message) {
        super(message);
    }
}
