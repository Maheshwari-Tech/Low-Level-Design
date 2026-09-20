package com.example.lld.order_processing_system.port;

import com.example.lld.order_processing_system.model.Address;

import java.util.List;

/** Boundary that requests warehouse work after commercial confirmation. */
@FunctionalInterface
public interface FulfilmentPort {
    void request(
            String orderId,
            Address address,
            List<FulfilmentLine> lines,
            String idempotencyKey);

    record FulfilmentLine(String lineId, String sku, int quantity) {
        public FulfilmentLine {
            if (lineId == null || lineId.isBlank() || sku == null || sku.isBlank()) {
                throw new IllegalArgumentException("lineId and sku must not be blank");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }
}
