package com.example.lld.order_processing_system.model;

import com.example.lld.order_processing_system.exception.InvalidOrderStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable order aggregate. Every command creates a new version after checking all
 * lifecycle and quantity invariants, so readers can safely retain returned snapshots.
 */
public final class Order {
    private final String orderId;
    private final String customerId;
    private final Address deliveryAddress;
    private final Instant createdAt;
    private final List<OrderLine> lines;
    private final OrderStatus status;
    private final PaymentStatus paymentStatus;
    private final FulfilmentStatus fulfilmentStatus;
    private final String inventoryReservationId;
    private final String paymentId;
    private final List<StatusChange> history;

    private Order(
            String orderId,
            String customerId,
            Address deliveryAddress,
            Instant createdAt,
            List<OrderLine> lines,
            OrderStatus status,
            PaymentStatus paymentStatus,
            FulfilmentStatus fulfilmentStatus,
            String inventoryReservationId,
            String paymentId,
            List<StatusChange> history) {
        this.orderId = requireText(orderId, "orderId");
        this.customerId = requireText(customerId, "customerId");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "deliveryAddress");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status, "status");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus");
        this.fulfilmentStatus = Objects.requireNonNull(fulfilmentStatus, "fulfilmentStatus");
        this.inventoryReservationId = inventoryReservationId;
        this.paymentId = paymentId;
        this.history = List.copyOf(history);
        if (this.lines.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }
        Currency currency = this.lines.get(0).unitPrice().currency();
        if (this.lines.stream().anyMatch(line -> !line.unitPrice().currency().equals(currency))) {
            throw new IllegalArgumentException("all lines must use one currency");
        }
    }

    public static Order create(
            String orderId,
            String customerId,
            Address deliveryAddress,
            List<OrderLine> lines,
            Instant createdAt) {
        StatusChange created = new StatusChange(
                "ORDER_CREATED", OrderStatus.PENDING, OrderStatus.PENDING, createdAt,
                "Immutable product, price, discount, and address snapshots captured");
        return new Order(
                orderId, customerId, deliveryAddress, createdAt, lines,
                OrderStatus.PENDING, PaymentStatus.NOT_STARTED,
                FulfilmentStatus.NOT_REQUESTED, null, null, List.of(created));
    }

    public Order confirm(String reservationId, String capturedPaymentId, Instant at) {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Order " + orderId + " cannot be confirmed from " + status);
        }
        List<StatusChange> updatedHistory = appendHistory(new StatusChange(
                "ORDER_CONFIRMED", status, OrderStatus.CONFIRMED, at,
                "Inventory reserved and payment captured"));
        return copy(
                lines, OrderStatus.CONFIRMED, PaymentStatus.CAPTURED,
                FulfilmentStatus.REQUESTED, requireText(reservationId, "reservationId"),
                requireText(capturedPaymentId, "capturedPaymentId"), updatedHistory);
    }

    public Order recordConfirmationFailure(
            String reason, boolean paymentDeclined, Instant at) {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "A confirmation failure cannot be recorded from " + status);
        }
        List<StatusChange> updatedHistory = appendHistory(new StatusChange(
                "CONFIRMATION_FAILED", status, status, at, reason));
        return copy(
                lines,
                status,
                paymentDeclined ? PaymentStatus.DECLINED : paymentStatus,
                fulfilmentStatus,
                null,
                null,
                updatedHistory);
    }

    public Order cancelLine(String lineId, int quantity, String reason, Instant at) {
        requireAfterConfirmation("cancel a line");
        OrderLine current = line(lineId);
        OrderLine replacement = current.cancel(quantity);
        List<OrderLine> updatedLines = replaceLine(replacement);
        OrderStatus nextStatus = deriveStatus(updatedLines);
        FulfilmentStatus nextFulfilment = deriveFulfilmentStatus(updatedLines);
        PaymentStatus nextPayment = nextStatus == OrderStatus.CANCELLED
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        List<StatusChange> updatedHistory = appendHistory(new StatusChange(
                "LINE_CANCELLED", status, nextStatus, at,
                reason + " [line=" + lineId + ", quantity=" + quantity + "]"));
        return copy(
                updatedLines, nextStatus, nextPayment, nextFulfilment,
                inventoryReservationId, paymentId, updatedHistory);
    }

    public Order cancelAll(String reason, Instant at) {
        if (status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Order " + orderId + " cannot be fully cancelled from " + status);
        }
        List<OrderLine> cancelledLines = lines.stream()
                .map(line -> line.openQuantity() == 0
                        ? line : line.cancel(line.openQuantity()))
                .toList();
        List<StatusChange> updatedHistory = appendHistory(new StatusChange(
                "ORDER_CANCELLED", status, OrderStatus.CANCELLED, at, reason));
        return copy(
                cancelledLines, OrderStatus.CANCELLED, PaymentStatus.REFUNDED,
                FulfilmentStatus.CANCELLED, inventoryReservationId, paymentId,
                updatedHistory);
    }

    public Order recordFulfilment(String lineId, int quantity, Instant at) {
        requireAfterConfirmation("record fulfilment");
        OrderLine replacement = line(lineId).fulfil(quantity);
        List<OrderLine> updatedLines = replaceLine(replacement);
        OrderStatus nextStatus = deriveStatus(updatedLines);
        FulfilmentStatus nextFulfilment = deriveFulfilmentStatus(updatedLines);
        List<StatusChange> updatedHistory = appendHistory(new StatusChange(
                "ITEMS_FULFILLED", status, nextStatus, at,
                "line=" + lineId + ", quantity=" + quantity));
        return copy(
                updatedLines, nextStatus, paymentStatus, nextFulfilment,
                inventoryReservationId, paymentId, updatedHistory);
    }

    public String orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public Address deliveryAddress() {
        return deliveryAddress;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public FulfilmentStatus fulfilmentStatus() {
        return fulfilmentStatus;
    }

    public String inventoryReservationId() {
        return inventoryReservationId;
    }

    public String paymentId() {
        return paymentId;
    }

    public List<StatusChange> history() {
        return history;
    }

    public OrderLine line(String lineId) {
        return lines.stream()
                .filter(line -> line.lineId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown line: " + lineId));
    }

    public Money subtotal() {
        return sum(lines.stream().map(OrderLine::grossTotal).toList());
    }

    public Money discountTotal() {
        return sum(lines.stream().map(OrderLine::discountTotal).toList());
    }

    public Money payableTotal() {
        return sum(lines.stream().map(OrderLine::netTotal).toList());
    }

    public Map<String, Integer> orderedSkuQuantities() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            quantities.merge(line.sku(), line.orderedQuantity(), Integer::sum);
        }
        return Map.copyOf(quantities);
    }

    public Map<String, Integer> openSkuQuantities() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            if (line.openQuantity() > 0) {
                quantities.merge(line.sku(), line.openQuantity(), Integer::sum);
            }
        }
        return Map.copyOf(quantities);
    }

    public Money openAmount() {
        List<Money> amounts = lines.stream()
                .filter(line -> line.openQuantity() > 0)
                .map(line -> line.netUnitPrice().multiply(line.openQuantity()))
                .toList();
        return sum(amounts);
    }

    private Money sum(List<Money> amounts) {
        Currency currency = lines.get(0).unitPrice().currency();
        Money result = Money.zero(currency);
        for (Money amount : amounts) {
            result = result.add(amount);
        }
        return result;
    }

    private void requireAfterConfirmation(String action) {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PARTIALLY_FULFILLED) {
            throw new InvalidOrderStateException(
                    "Cannot " + action + " for order " + orderId + " in " + status);
        }
    }

    private List<OrderLine> replaceLine(OrderLine replacement) {
        List<OrderLine> updated = new ArrayList<>(lines.size());
        for (OrderLine candidate : lines) {
            updated.add(candidate.lineId().equals(replacement.lineId()) ? replacement : candidate);
        }
        return List.copyOf(updated);
    }

    private static OrderStatus deriveStatus(List<OrderLine> updatedLines) {
        int fulfilled = updatedLines.stream().mapToInt(OrderLine::fulfilledQuantity).sum();
        int open = updatedLines.stream().mapToInt(OrderLine::openQuantity).sum();
        if (open == 0) {
            return fulfilled == 0 ? OrderStatus.CANCELLED : OrderStatus.FULFILLED;
        }
        return fulfilled > 0 ? OrderStatus.PARTIALLY_FULFILLED : OrderStatus.CONFIRMED;
    }

    private static FulfilmentStatus deriveFulfilmentStatus(List<OrderLine> updatedLines) {
        int fulfilled = updatedLines.stream().mapToInt(OrderLine::fulfilledQuantity).sum();
        int open = updatedLines.stream().mapToInt(OrderLine::openQuantity).sum();
        if (open == 0) {
            return fulfilled == 0 ? FulfilmentStatus.CANCELLED : FulfilmentStatus.FULFILLED;
        }
        return fulfilled > 0 ? FulfilmentStatus.PARTIALLY_FULFILLED : FulfilmentStatus.REQUESTED;
    }

    private List<StatusChange> appendHistory(StatusChange change) {
        List<StatusChange> updated = new ArrayList<>(history);
        updated.add(change);
        return List.copyOf(updated);
    }

    private Order copy(
            List<OrderLine> nextLines,
            OrderStatus nextStatus,
            PaymentStatus nextPaymentStatus,
            FulfilmentStatus nextFulfilmentStatus,
            String nextReservationId,
            String nextPaymentId,
            List<StatusChange> nextHistory) {
        return new Order(
                orderId, customerId, deliveryAddress, createdAt, nextLines, nextStatus,
                nextPaymentStatus, nextFulfilmentStatus, nextReservationId, nextPaymentId,
                nextHistory);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "Order{" + orderId + ", status=" + status + ", payment=" + paymentStatus
                + ", fulfilment=" + fulfilmentStatus + ", total=" + payableTotal() + '}';
    }
}
