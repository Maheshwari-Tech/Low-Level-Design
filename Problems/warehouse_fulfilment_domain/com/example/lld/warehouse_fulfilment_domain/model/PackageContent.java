package com.example.lld.warehouse_fulfilment_domain.model;

public record PackageContent(String orderLineId, String sku, int quantity) {
    public PackageContent {
        if (orderLineId == null || orderLineId.isBlank() || sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("orderLineId and sku must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
