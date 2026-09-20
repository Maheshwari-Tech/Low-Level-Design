package com.example.lld.order_processing_system.model;

/** Input used to create an immutable product/price snapshot. */
public record OrderLineRequest(String sku, String productName, int quantity, Money unitPrice) {
    public OrderLineRequest {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null || unitPrice.isNegative()) {
            throw new IllegalArgumentException("unitPrice must be non-negative");
        }
    }
}
