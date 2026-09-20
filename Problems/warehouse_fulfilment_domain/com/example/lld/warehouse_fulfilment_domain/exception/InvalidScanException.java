package com.example.lld.warehouse_fulfilment_domain.exception;

public final class InvalidScanException extends WarehouseDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidScanException(String message) {
        super(message);
    }
}
