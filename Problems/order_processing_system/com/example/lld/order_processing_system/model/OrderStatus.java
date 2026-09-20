package com.example.lld.order_processing_system.model;

/** Customer-visible commercial lifecycle, deliberately separate from payment and fulfilment. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PARTIALLY_FULFILLED,
    FULFILLED,
    CANCELLED
}
