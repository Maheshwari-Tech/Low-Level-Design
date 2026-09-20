package com.example.lld.inventory_reservation_service.service;

import com.example.lld.inventory_reservation_service.exception.IdempotencyConflictException;
import com.example.lld.inventory_reservation_service.exception.InvalidReservationStateException;
import com.example.lld.inventory_reservation_service.exception.ReservationExpiredException;
import com.example.lld.inventory_reservation_service.exception.ReservationNotFoundException;
import com.example.lld.inventory_reservation_service.model.InventoryBalance;
import com.example.lld.inventory_reservation_service.model.InventoryKey;
import com.example.lld.inventory_reservation_service.model.Reservation;
import com.example.lld.inventory_reservation_service.model.ReservationLine;
import com.example.lld.inventory_reservation_service.model.ReservationStatus;
import com.example.lld.inventory_reservation_service.port.AllocationStrategy;
import com.example.lld.inventory_reservation_service.port.IdGenerator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe in-memory reference implementation. One fair lock is the transaction
 * boundary, making multi-SKU checks and counter changes all-or-nothing and giving a
 * deterministic winner to confirmation, release, and expiry.
 */
public final class InventoryReservationService {
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final AllocationStrategy allocationStrategy;
    private final ReentrantLock transactionLock = new ReentrantLock(true);
    private final Map<InventoryKey, BalanceState> balances = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Map<String, IdempotencyEntry> completedCommands = new HashMap<>();

    public InventoryReservationService(
            Clock clock,
            IdGenerator idGenerator,
            AllocationStrategy allocationStrategy) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy, "allocationStrategy");
    }

    public void addStock(String sku, String location, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        InventoryKey key = new InventoryKey(sku, location);
        transactionLock.lock();
        try {
            BalanceState state = balances.computeIfAbsent(key, ignored -> new BalanceState());
            state.onHand = Math.addExact(state.onHand, quantity);
        } finally {
            transactionLock.unlock();
        }
    }

    public Reservation reserve(
            String idempotencyKey,
            Map<String, Integer> requestedQuantities,
            Duration timeToLive) {
        requireKey(idempotencyKey);
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
        Map<String, Integer> requested = validatedRequest(requestedQuantities);
        String fingerprint = new TreeMap<>(requested) + "|ttl=" + timeToLive;
        transactionLock.lock();
        try {
            Reservation duplicate = duplicateResult("reserve", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Instant now = clock.instant();
            expireDueLocked(now);
            List<ReservationLine> allocation = allocationStrategy.allocate(
                    requested, balanceSnapshots());
            Map<InventoryKey, Integer> byBalance = validateAllocation(requested, allocation);

            // No counter changes occur before the complete allocation has been validated.
            for (Map.Entry<InventoryKey, Integer> entry : byBalance.entrySet()) {
                BalanceState balance = balances.get(entry.getKey());
                balance.reserved = Math.addExact(balance.reserved, entry.getValue());
            }
            Reservation reservation = Reservation.active(
                    idGenerator.nextId(), allocation, now, now.plus(timeToLive));
            reservations.put(reservation.reservationId(), reservation);
            remember("reserve", idempotencyKey, fingerprint, reservation);
            return reservation;
        } finally {
            transactionLock.unlock();
        }
    }

    public Reservation confirm(String idempotencyKey, String reservationId) {
        requireKey(idempotencyKey);
        String fingerprint = requireText(reservationId, "reservationId");
        transactionLock.lock();
        try {
            Reservation duplicate = duplicateResult("confirm", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Reservation current = requireReservation(reservationId);
            if (current.status() == ReservationStatus.CONFIRMED) {
                remember("confirm", idempotencyKey, fingerprint, current);
                return current;
            }
            if (current.status() != ReservationStatus.ACTIVE) {
                throw invalidTransition(current, "confirm");
            }
            if (current.isDueAt(clock.instant())) {
                Reservation expired = expire(current);
                reservations.put(reservationId, expired);
                throw new ReservationExpiredException(reservationId);
            }
            for (ReservationLine line : current.lines()) {
                BalanceState balance = balances.get(line.inventoryKey());
                balance.reserved -= line.quantity();
                balance.onHand -= line.quantity();
            }
            Reservation confirmed = current.transitionTo(ReservationStatus.CONFIRMED);
            reservations.put(reservationId, confirmed);
            remember("confirm", idempotencyKey, fingerprint, confirmed);
            return confirmed;
        } finally {
            transactionLock.unlock();
        }
    }

    public Reservation release(String idempotencyKey, String reservationId) {
        requireKey(idempotencyKey);
        String fingerprint = requireText(reservationId, "reservationId");
        transactionLock.lock();
        try {
            Reservation duplicate = duplicateResult("release", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Reservation current = requireReservation(reservationId);
            if (current.status() == ReservationStatus.RELEASED) {
                remember("release", idempotencyKey, fingerprint, current);
                return current;
            }
            if (current.status() != ReservationStatus.ACTIVE) {
                throw invalidTransition(current, "release");
            }
            if (current.isDueAt(clock.instant())) {
                Reservation expired = expire(current);
                reservations.put(reservationId, expired);
                throw invalidTransition(expired, "release");
            }
            releaseHeldCounters(current);
            Reservation released = current.transitionTo(ReservationStatus.RELEASED);
            reservations.put(reservationId, released);
            remember("release", idempotencyKey, fingerprint, released);
            return released;
        } finally {
            transactionLock.unlock();
        }
    }

    /** Expires every reservation whose expiry is less than or equal to the injected clock. */
    public int expireDueReservations() {
        transactionLock.lock();
        try {
            return expireDueLocked(clock.instant());
        } finally {
            transactionLock.unlock();
        }
    }

    public Reservation getReservation(String reservationId) {
        transactionLock.lock();
        try {
            expireDueLocked(clock.instant());
            return requireReservation(reservationId);
        } finally {
            transactionLock.unlock();
        }
    }

    public InventoryBalance getBalance(String sku, String location) {
        InventoryKey key = new InventoryKey(sku, location);
        transactionLock.lock();
        try {
            expireDueLocked(clock.instant());
            BalanceState state = balances.get(key);
            return state == null
                    ? new InventoryBalance(key, 0, 0)
                    : state.snapshot(key);
        } finally {
            transactionLock.unlock();
        }
    }

    public List<InventoryBalance> balances() {
        transactionLock.lock();
        try {
            expireDueLocked(clock.instant());
            return balanceSnapshots();
        } finally {
            transactionLock.unlock();
        }
    }

    private Map<InventoryKey, Integer> validateAllocation(
            Map<String, Integer> requested,
            List<ReservationLine> allocation) {
        Map<String, Integer> bySku = new HashMap<>();
        Map<InventoryKey, Integer> byBalance = new LinkedHashMap<>();
        for (ReservationLine line : allocation) {
            if (!requested.containsKey(line.sku())) {
                throw new IllegalStateException(
                        "Allocation strategy returned unrequested SKU " + line.sku());
            }
            bySku.merge(line.sku(), line.quantity(), Math::addExact);
            byBalance.merge(line.inventoryKey(), line.quantity(), Math::addExact);
        }
        if (!requested.equals(new TreeMap<>(bySku))) {
            throw new IllegalStateException(
                    "Allocation strategy must allocate every requested quantity exactly");
        }
        for (Map.Entry<InventoryKey, Integer> entry : byBalance.entrySet()) {
            BalanceState state = balances.get(entry.getKey());
            if (state == null || entry.getValue() > state.available()) {
                throw new IllegalStateException(
                        "Allocation strategy exceeded availability for " + entry.getKey());
            }
        }
        return byBalance;
    }

    private int expireDueLocked(Instant now) {
        List<String> dueIds = reservations.values().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.isDueAt(now))
                .map(Reservation::reservationId)
                .toList();
        for (String reservationId : dueIds) {
            Reservation current = reservations.get(reservationId);
            reservations.put(reservationId, expire(current));
        }
        return dueIds.size();
    }

    private Reservation expire(Reservation reservation) {
        releaseHeldCounters(reservation);
        return reservation.transitionTo(ReservationStatus.EXPIRED);
    }

    private void releaseHeldCounters(Reservation reservation) {
        for (ReservationLine line : reservation.lines()) {
            BalanceState balance = balances.get(line.inventoryKey());
            balance.reserved -= line.quantity();
            if (balance.reserved < 0) {
                throw new IllegalStateException("reserved inventory became negative");
            }
        }
    }

    private List<InventoryBalance> balanceSnapshots() {
        List<InventoryBalance> result = new ArrayList<>(balances.size());
        for (Map.Entry<InventoryKey, BalanceState> entry : balances.entrySet()) {
            result.add(entry.getValue().snapshot(entry.getKey()));
        }
        result.sort((left, right) -> left.key().compareTo(right.key()));
        return List.copyOf(result);
    }

    private Reservation requireReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new ReservationNotFoundException(reservationId);
        }
        return reservation;
    }

    private Reservation duplicateResult(String operation, String key, String fingerprint) {
        IdempotencyEntry entry = completedCommands.get(operation + ':' + key);
        if (entry == null) {
            return null;
        }
        if (!entry.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        return entry.result();
    }

    private void remember(
            String operation, String key, String fingerprint, Reservation result) {
        completedCommands.put(operation + ':' + key, new IdempotencyEntry(fingerprint, result));
    }

    private static InvalidReservationStateException invalidTransition(
            Reservation reservation, String action) {
        return new InvalidReservationStateException(
                "Cannot " + action + " reservation " + reservation.reservationId()
                        + " in " + reservation.status());
    }

    private static Map<String, Integer> validatedRequest(Map<String, Integer> request) {
        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("at least one SKU quantity is required");
        }
        Map<String, Integer> result = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : request.entrySet()) {
            String sku = requireText(entry.getKey(), "sku");
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive for " + sku);
            }
            result.put(sku, quantity);
        }
        return Map.copyOf(result);
    }

    private static void requireKey(String key) {
        requireText(key, "idempotencyKey");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static final class BalanceState {
        private int onHand;
        private int reserved;

        private int available() {
            return onHand - reserved;
        }

        private InventoryBalance snapshot(InventoryKey key) {
            return new InventoryBalance(key, onHand, reserved);
        }
    }

    private record IdempotencyEntry(String fingerprint, Reservation result) {
    }
}
