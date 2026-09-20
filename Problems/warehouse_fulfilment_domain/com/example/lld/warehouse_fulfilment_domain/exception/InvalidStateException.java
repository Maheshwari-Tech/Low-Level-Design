package com.example.lld.warehouse_fulfilment_domain.exception;

public final class InvalidStateException extends WarehouseDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidStateException(String message) {
        super(message);
    }
}
