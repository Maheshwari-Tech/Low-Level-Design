package com.example.lld.inventory_reservation_service.exception;

public final class InsufficientInventoryException extends InventoryDomainException {
    private static final long serialVersionUID = 1L;

    public InsufficientInventoryException(String sku, int requested, int available) {
        super("Insufficient inventory for " + sku + ": requested=" + requested
                + ", available=" + available);
    }
}
