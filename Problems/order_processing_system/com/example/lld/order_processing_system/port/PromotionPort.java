package com.example.lld.order_processing_system.port;

import com.example.lld.order_processing_system.model.Money;

/** Supplies a per-unit discount that the order stores as an immutable snapshot. */
@FunctionalInterface
public interface PromotionPort {
    Money unitDiscount(
            String customerId, String sku, int quantity, Money unitPrice);
}
