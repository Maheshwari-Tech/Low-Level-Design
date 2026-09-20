package com.example.lld.warehouse_fulfilment_domain.exception;

public class WarehouseDomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WarehouseDomainException(String message) {
        super(message);
    }
}
