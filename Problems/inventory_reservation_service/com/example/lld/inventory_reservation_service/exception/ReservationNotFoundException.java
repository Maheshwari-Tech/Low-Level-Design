package com.example.lld.inventory_reservation_service.exception;

public final class ReservationNotFoundException extends InventoryDomainException {
    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(String reservationId) {
        super("Reservation not found: " + reservationId);
    }
}
