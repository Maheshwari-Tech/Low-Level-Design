package com.example.lld.product_catalog_service.exception;

public final class VersionConflictException extends CatalogException {
    private static final long serialVersionUID = 1L;

    public VersionConflictException(String aggregateType, String id, long expected, long actual) {
        super(aggregateType + " " + id + " version conflict: expected " + expected + " but was " + actual);
    }
}
