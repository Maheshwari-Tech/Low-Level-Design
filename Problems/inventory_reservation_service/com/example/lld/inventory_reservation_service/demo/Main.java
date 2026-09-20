package com.example.lld.inventory_reservation_service.demo;

import com.example.lld.inventory_reservation_service.exception.InsufficientInventoryException;
import com.example.lld.inventory_reservation_service.model.InventoryBalance;
import com.example.lld.inventory_reservation_service.model.Reservation;
import com.example.lld.inventory_reservation_service.model.ReservationStatus;
import com.example.lld.inventory_reservation_service.service.FirstFitAllocationStrategy;
import com.example.lld.inventory_reservation_service.service.InventoryReservationService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Executable success, rollback, retry, expiry, and concurrency scenarios. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-08-09T10:15:30Z"), ZoneOffset.UTC);
        AtomicInteger ids = new AtomicInteger();
        InventoryReservationService service = new InventoryReservationService(
                clock,
                () -> "reservation-" + ids.incrementAndGet(),
                new FirstFitAllocationStrategy());

        service.addStock("BOOK", "BLR-1", 3);
        Reservation hold = service.reserve(
                "reserve-100", Map.of("BOOK", 2), Duration.ofMinutes(5));
        Reservation duplicateHold = service.reserve(
                "reserve-100", Map.of("BOOK", 2), Duration.ofMinutes(5));
        check(duplicateHold == hold, "reserve retry returns its original immutable result");
        check(service.getBalance("BOOK", "BLR-1").reserved() == 2,
                "reserve retry does not hold stock twice");

        Reservation confirmed = service.confirm("confirm-100", hold.reservationId());
        Reservation duplicateConfirmation = service.confirm(
                "confirm-100", hold.reservationId());
        InventoryBalance afterConfirmation = service.getBalance("BOOK", "BLR-1");
        check(confirmed.status() == ReservationStatus.CONFIRMED,
                "active reservation confirms");
        check(duplicateConfirmation == confirmed,
                "confirm retry returns the original result");
        check(afterConfirmation.onHand() == 1 && afterConfirmation.reserved() == 0,
                "confirmation consumes held stock exactly once");

        service.addStock("MUG", "BLR-1", 1);
        try {
            service.reserve(
                    "reserve-atomic-failure",
                    Map.of("MUG", 1, "MISSING", 1),
                    Duration.ofMinutes(1));
            throw new AssertionError("multi-line reservation should fail");
        } catch (InsufficientInventoryException expected) {
            check(service.getBalance("MUG", "BLR-1").available() == 1,
                    "failed multi-line reservation leaves no partial hold");
        }

        service.addStock("EXPIRING", "BLR-1", 1);
        Reservation expiring = service.reserve(
                "reserve-expiring", Map.of("EXPIRING", 1), Duration.ofSeconds(5));
        check(service.getBalance("EXPIRING", "BLR-1").available() == 0,
                "active hold reduces availability");
        clock.advance(Duration.ofSeconds(5));
        check(service.expireDueReservations() == 1,
                "equality with expiresAt is the expiry boundary");
        check(service.getReservation(expiring.reservationId()).status()
                        == ReservationStatus.EXPIRED,
                "abandoned hold becomes expired");
        check(service.getBalance("EXPIRING", "BLR-1").available() == 1,
                "expiry restores availability");

        service.addStock("LAST", "BLR-1", 1);
        int competingSuccesses = competeForFinalUnit(service);
        check(competingSuccesses == 1,
                "at most one concurrent caller reserves the final unit");
        check(service.getBalance("LAST", "BLR-1").reserved() == 1,
                "the concurrency winner holds exactly one unit");

        service.addStock("RACE", "BLR-1", 1);
        Reservation racing = service.reserve(
                "reserve-race", Map.of("RACE", 1), Duration.ofSeconds(10));
        clock.advance(Duration.ofSeconds(10));
        raceConfirmationAgainstExpiry(service, racing.reservationId());
        check(service.getReservation(racing.reservationId()).status()
                        == ReservationStatus.EXPIRED,
                "expiry deterministically wins at the TTL boundary");
        check(service.getBalance("RACE", "BLR-1").available() == 1,
                "confirm-versus-expiry race releases the hold once");

        System.out.println("Inventory Reservation Service demo passed");
        System.out.println("  confirmed: " + confirmed.reservationId());
        System.out.println("  expired: " + expiring.reservationId());
        System.out.println("  final-unit successes: " + competingSuccesses + "/2");
        System.out.println("  boundary-race result: "
                + service.getReservation(racing.reservationId()).status());
    }

    private static int competeForFinalUnit(InventoryReservationService service)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Reservation> first = pool.submit(() -> service.reserve(
                    "last-a", Map.of("LAST", 1), Duration.ofMinutes(1)));
            Future<Reservation> second = pool.submit(() -> service.reserve(
                    "last-b", Map.of("LAST", 1), Duration.ofMinutes(1)));
            int successes = 0;
            for (Future<Reservation> result : List.of(first, second)) {
                try {
                    result.get();
                    successes++;
                } catch (ExecutionException expectedLoser) {
                    if (!(expectedLoser.getCause() instanceof InsufficientInventoryException)) {
                        throw expectedLoser;
                    }
                }
            }
            return successes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void raceConfirmationAgainstExpiry(
            InventoryReservationService service, String reservationId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> confirmation = pool.submit(
                    () -> service.confirm("confirm-race", reservationId));
            Future<?> cleanup = pool.submit(service::expireDueReservations);
            for (Future<?> result : List.of(confirmation, cleanup)) {
                try {
                    result.get();
                } catch (ExecutionException expectedConfirmationLoss) {
                    if (!(expectedConfirmationLoss.getCause() instanceof RuntimeException)) {
                        throw expectedConfirmationLoss;
                    }
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Small controllable clock used to exercise TTL boundaries without sleeping. */
    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;
        private final ZoneId zone;

        private MutableClock(Instant initial, ZoneId zone) {
            this(new AtomicReference<>(initial), zone);
        }

        private MutableClock(AtomicReference<Instant> current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(current, newZone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        private void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }
    }
}
