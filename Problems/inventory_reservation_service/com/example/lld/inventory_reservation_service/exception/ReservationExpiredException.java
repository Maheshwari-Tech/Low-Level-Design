package com.example.lld.inventory_reservation_service.exception;

public final class ReservationExpiredException extends InventoryDomainException {
    private static final long serialVersionUID = 1L;

    public ReservationExpiredException(String reservationId) {
        super("Reservation expired before it could be confirmed: " + reservationId);
    }
}
