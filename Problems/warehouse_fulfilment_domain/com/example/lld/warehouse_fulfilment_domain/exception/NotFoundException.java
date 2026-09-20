package com.example.lld.warehouse_fulfilment_domain.exception;

public final class NotFoundException extends WarehouseDomainException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String type, String id) {
        super(type + " not found: " + id);
    }
}
