package com.example.lld.order_processing_system.model;

import com.example.lld.order_processing_system.exception.InvalidOrderStateException;

import java.util.Objects;

/** Immutable line snapshot; only fulfilment and cancellation counters can evolve via copies. */
public record OrderLine(
        String lineId,
        String sku,
        String productName,
        int orderedQuantity,
        Money unitPrice,
        Money unitDiscount,
        int fulfilledQuantity,
        int cancelledQuantity) {

    public OrderLine {
        lineId = requireText(lineId, "lineId");
        sku = requireText(sku, "sku");
        productName = requireText(productName, "productName");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(unitDiscount, "unitDiscount");
        if (orderedQuantity <= 0) {
            throw new IllegalArgumentException("orderedQuantity must be positive");
        }
        if (unitPrice.isNegative() || unitDiscount.isNegative()
                || unitDiscount.compareTo(unitPrice) > 0) {
            throw new IllegalArgumentException("discount must be between zero and unit price");
        }
        if (fulfilledQuantity < 0 || cancelledQuantity < 0
                || fulfilledQuantity + cancelledQuantity > orderedQuantity) {
            throw new IllegalArgumentException("line quantities violate the order invariant");
        }
    }

    public static OrderLine create(
            String lineId, OrderLineRequest request, Money unitDiscount) {
        return new OrderLine(
                lineId,
                request.sku(),
                request.productName(),
                request.quantity(),
                request.unitPrice(),
                unitDiscount,
                0,
                0);
    }

    public int openQuantity() {
        return orderedQuantity - fulfilledQuantity - cancelledQuantity;
    }

    public Money netUnitPrice() {
        return unitPrice.subtract(unitDiscount);
    }

    public Money grossTotal() {
        return unitPrice.multiply(orderedQuantity);
    }

    public Money discountTotal() {
        return unitDiscount.multiply(orderedQuantity);
    }

    public Money netTotal() {
        return netUnitPrice().multiply(orderedQuantity);
    }

    public Money amountFor(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return netUnitPrice().multiply(quantity);
    }

    public OrderLine fulfil(int quantity) {
        requireWithinOpenQuantity(quantity, "fulfil");
        return new OrderLine(
                lineId, sku, productName, orderedQuantity, unitPrice, unitDiscount,
                fulfilledQuantity + quantity, cancelledQuantity);
    }

    public OrderLine cancel(int quantity) {
        requireWithinOpenQuantity(quantity, "cancel");
        return new OrderLine(
                lineId, sku, productName, orderedQuantity, unitPrice, unitDiscount,
                fulfilledQuantity, cancelledQuantity + quantity);
    }

    public OrderLineStatus status() {
        if (cancelledQuantity == orderedQuantity) {
            return OrderLineStatus.CANCELLED;
        }
        if (fulfilledQuantity + cancelledQuantity == orderedQuantity) {
            return fulfilledQuantity == 0
                    ? OrderLineStatus.CANCELLED : OrderLineStatus.FULFILLED;
        }
        if (fulfilledQuantity > 0) {
            return OrderLineStatus.PARTIALLY_FULFILLED;
        }
        if (cancelledQuantity > 0) {
            return OrderLineStatus.PARTIALLY_CANCELLED;
        }
        return OrderLineStatus.OPEN;
    }

    private void requireWithinOpenQuantity(int quantity, String action) {
        if (quantity <= 0 || quantity > openQuantity()) {
            throw new InvalidOrderStateException(
                    "Cannot " + action + " " + quantity + " unit(s) of line " + lineId
                            + "; open quantity is " + openQuantity());
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
