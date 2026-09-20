package com.example.lld.inventory_reservation_service.model;

import com.example.lld.inventory_reservation_service.exception.InvalidReservationStateException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable reservation aggregate with one explicit terminal transition. */
public record Reservation(
        String reservationId,
        List<ReservationLine> lines,
        Instant createdAt,
        Instant expiresAt,
        ReservationStatus status) {

    public Reservation {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("a reservation must contain allocations");
        }
        if (createdAt == null || expiresAt == null || status == null) {
            throw new IllegalArgumentException("timestamps and status are required");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
    }

    public static Reservation active(
            String reservationId,
            List<ReservationLine> lines,
            Instant createdAt,
            Instant expiresAt) {
        return new Reservation(
                reservationId, lines, createdAt, expiresAt, ReservationStatus.ACTIVE);
    }

    public boolean isDueAt(Instant instant) {
        // Documented boundary rule: equality belongs to expiry, not confirmation.
        return !instant.isBefore(expiresAt);
    }

    public Reservation transitionTo(ReservationStatus target) {
        if (target == status) {
            return this;
        }
        if (status != ReservationStatus.ACTIVE || target == ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(
                    "Reservation " + reservationId + " cannot transition from "
                            + status + " to " + target);
        }
        return new Reservation(reservationId, lines, createdAt, expiresAt, target);
    }

    public Map<String, Integer> allocatedSkuQuantities() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (ReservationLine line : lines) {
            quantities.merge(line.sku(), line.quantity(), Integer::sum);
        }
        return Map.copyOf(quantities);
    }
}
