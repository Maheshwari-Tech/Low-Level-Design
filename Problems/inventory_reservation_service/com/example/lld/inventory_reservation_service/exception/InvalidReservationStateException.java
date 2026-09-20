package com.example.lld.inventory_reservation_service.exception;

public final class InvalidReservationStateException extends InventoryDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidReservationStateException(String message) {
        super(message);
    }
}
