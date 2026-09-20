package com.example.lld.order_processing_system.service;

import com.example.lld.order_processing_system.exception.ConfirmationException;
import com.example.lld.order_processing_system.exception.IdempotencyConflictException;
import com.example.lld.order_processing_system.exception.OrderNotFoundException;
import com.example.lld.order_processing_system.model.Address;
import com.example.lld.order_processing_system.model.Money;
import com.example.lld.order_processing_system.model.Order;
import com.example.lld.order_processing_system.model.OrderLine;
import com.example.lld.order_processing_system.model.OrderLineRequest;
import com.example.lld.order_processing_system.model.OrderStatus;
import com.example.lld.order_processing_system.port.FulfilmentPort;
import com.example.lld.order_processing_system.port.IdGenerator;
import com.example.lld.order_processing_system.port.InventoryPort;
import com.example.lld.order_processing_system.port.PaymentPort;
import com.example.lld.order_processing_system.port.PromotionPort;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory application service. A fair lock makes each command atomic for this
 * reference implementation; persistent deployments can replace it with aggregate
 * version checks and a transactional idempotency table/outbox.
 */
public final class OrderService {
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final InventoryPort inventory;
    private final PaymentPort payments;
    private final PromotionPort promotions;
    private final FulfilmentPort fulfilment;
    private final ReentrantLock commandLock = new ReentrantLock(true);
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, IdempotencyEntry> completedCommands = new HashMap<>();
    private final Set<String> fulfilmentRequestsSent = new HashSet<>();

    public OrderService(
            Clock clock,
            IdGenerator idGenerator,
            InventoryPort inventory,
            PaymentPort payments,
            PromotionPort promotions,
            FulfilmentPort fulfilment) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.payments = Objects.requireNonNull(payments, "payments");
        this.promotions = Objects.requireNonNull(promotions, "promotions");
        this.fulfilment = Objects.requireNonNull(fulfilment, "fulfilment");
    }

    public Order createOrder(
            String idempotencyKey,
            String customerId,
            Address address,
            List<OrderLineRequest> requests) {
        requireKey(idempotencyKey);
        Objects.requireNonNull(address, "address");
        List<OrderLineRequest> requestSnapshot = List.copyOf(requests);
        if (requestSnapshot.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }
        String fingerprint = customerId + '|' + address + '|' + requestSnapshot;
        commandLock.lock();
        try {
            Order duplicate = duplicateResult("create", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            List<OrderLine> lines = new ArrayList<>(requestSnapshot.size());
            for (OrderLineRequest request : requestSnapshot) {
                Money discount = promotions.unitDiscount(
                        customerId, request.sku(), request.quantity(), request.unitPrice());
                lines.add(OrderLine.create(idGenerator.nextId("line"), request, discount));
            }
            Order created = Order.create(
                    idGenerator.nextId("order"), customerId, address, lines, clock.instant());
            orders.put(created.orderId(), created);
            remember("create", idempotencyKey, fingerprint, created);
            return created;
        } finally {
            commandLock.unlock();
        }
    }

    public Order confirmOrder(String idempotencyKey, String orderId) {
        requireKey(idempotencyKey);
        String fingerprint = requireText(orderId, "orderId");
        commandLock.lock();
        try {
            Order duplicate = duplicateResult("confirm", idempotencyKey, fingerprint);
            if (duplicate != null) {
                requestFulfilmentIfNeeded(duplicate);
                return duplicate;
            }
            Order current = requireOrder(orderId);
            if (current.status() != OrderStatus.PENDING) {
                remember("confirm", idempotencyKey, fingerprint, current);
                requestFulfilmentIfNeeded(current);
                return current;
            }

            InventoryPort.InventoryHold hold;
            try {
                hold = inventory.reserve(
                        orderId,
                        current.orderedSkuQuantities(),
                        "inventory-confirm:" + orderId + ':' + idempotencyKey);
            } catch (RuntimeException failure) {
                Order failed = current.recordConfirmationFailure(
                        "Inventory reservation failed: " + failure.getMessage(), false,
                        clock.instant());
                orders.put(orderId, failed);
                throw new ConfirmationException("Could not reserve inventory for " + orderId, failure);
            }

            PaymentPort.PaymentReceipt receipt;
            try {
                receipt = payments.charge(
                        orderId,
                        current.payableTotal(),
                        "payment-confirm:" + orderId + ':' + idempotencyKey);
            } catch (RuntimeException failure) {
                try {
                    inventory.release(
                            hold.reservationId(),
                            "inventory-release:" + orderId + ':' + idempotencyKey);
                } catch (RuntimeException releaseFailure) {
                    failure.addSuppressed(releaseFailure);
                }
                Order failed = current.recordConfirmationFailure(
                        "Payment failed; inventory release requested: " + failure.getMessage(),
                        true, clock.instant());
                orders.put(orderId, failed);
                throw new ConfirmationException("Could not capture payment for " + orderId, failure);
            }

            Order confirmed = current.confirm(
                    hold.reservationId(), receipt.paymentId(), clock.instant());
            orders.put(orderId, confirmed);
            remember("confirm", idempotencyKey, fingerprint, confirmed);
            requestFulfilmentIfNeeded(confirmed);
            return confirmed;
        } finally {
            commandLock.unlock();
        }
    }

    public Order cancelLine(
            String idempotencyKey,
            String orderId,
            String lineId,
            int quantity,
            String reason) {
        requireKey(idempotencyKey);
        String fingerprint = orderId + '|' + lineId + '|' + quantity + '|' + reason;
        commandLock.lock();
        try {
            Order duplicate = duplicateResult("cancel", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Order current = requireOrder(orderId);
            Order proposed = current.cancelLine(lineId, quantity, reason, clock.instant());
            Money refund = current.line(lineId).amountFor(quantity);
            payments.refund(
                    current.paymentId(), refund,
                    "payment-cancel:" + orderId + ':' + lineId + ':' + idempotencyKey);
            inventory.cancelCommitted(
                    current.inventoryReservationId(),
                    Map.of(current.line(lineId).sku(), quantity),
                    "inventory-cancel:" + orderId + ':' + lineId + ':' + idempotencyKey);
            orders.put(orderId, proposed);
            remember("cancel", idempotencyKey, fingerprint, proposed);
            return proposed;
        } finally {
            commandLock.unlock();
        }
    }

    public Order cancelOrder(String idempotencyKey, String orderId, String reason) {
        requireKey(idempotencyKey);
        String fingerprint = orderId + '|' + reason;
        commandLock.lock();
        try {
            Order duplicate = duplicateResult("cancel-order", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Order current = requireOrder(orderId);
            Order proposed = current.cancelAll(reason, clock.instant());
            payments.refund(
                    current.paymentId(), current.openAmount(),
                    "payment-cancel-order:" + orderId + ':' + idempotencyKey);
            inventory.cancelCommitted(
                    current.inventoryReservationId(), current.openSkuQuantities(),
                    "inventory-cancel-order:" + orderId + ':' + idempotencyKey);
            orders.put(orderId, proposed);
            remember("cancel-order", idempotencyKey, fingerprint, proposed);
            return proposed;
        } finally {
            commandLock.unlock();
        }
    }

    public Order recordFulfilment(
            String idempotencyKey, String orderId, String lineId, int quantity) {
        requireKey(idempotencyKey);
        String fingerprint = orderId + '|' + lineId + '|' + quantity;
        commandLock.lock();
        try {
            Order duplicate = duplicateResult("fulfil", idempotencyKey, fingerprint);
            if (duplicate != null) {
                return duplicate;
            }
            Order updated = requireOrder(orderId).recordFulfilment(
                    lineId, quantity, clock.instant());
            orders.put(orderId, updated);
            remember("fulfil", idempotencyKey, fingerprint, updated);
            return updated;
        } finally {
            commandLock.unlock();
        }
    }

    public Order getOrder(String orderId) {
        commandLock.lock();
        try {
            return requireOrder(orderId);
        } finally {
            commandLock.unlock();
        }
    }

    private Order requireOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    private void requestFulfilmentIfNeeded(Order order) {
        if (order.status() == OrderStatus.PENDING
                || fulfilmentRequestsSent.contains(order.orderId())) {
            return;
        }
        List<FulfilmentPort.FulfilmentLine> fulfilmentLines = order.lines().stream()
                .map(line -> new FulfilmentPort.FulfilmentLine(
                        line.lineId(), line.sku(), line.orderedQuantity()))
                .toList();
        fulfilment.request(
                order.orderId(), order.deliveryAddress(), fulfilmentLines,
                "fulfilment-request:" + order.orderId());
        fulfilmentRequestsSent.add(order.orderId());
    }

    private Order duplicateResult(String operation, String key, String fingerprint) {
        IdempotencyEntry entry = completedCommands.get(operation + ':' + key);
        if (entry == null) {
            return null;
        }
        if (!entry.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        return entry.result();
    }

    private void remember(String operation, String key, String fingerprint, Order result) {
        completedCommands.put(operation + ':' + key, new IdempotencyEntry(fingerprint, result));
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

    private record IdempotencyEntry(String fingerprint, Order result) {
    }
}
