package com.example.lld.order_processing_system.port;

import java.util.Map;

/** Boundary to an inventory service; implementations must honor the supplied key. */
public interface InventoryPort {
    InventoryHold reserve(String orderId, Map<String, Integer> skuQuantities, String idempotencyKey);

    void release(String reservationId, String idempotencyKey);

    void cancelCommitted(
            String reservationId,
            Map<String, Integer> skuQuantities,
            String idempotencyKey);

    record InventoryHold(String reservationId) {
        public InventoryHold {
            if (reservationId == null || reservationId.isBlank()) {
                throw new IllegalArgumentException("reservationId must not be blank");
            }
        }
    }
}
