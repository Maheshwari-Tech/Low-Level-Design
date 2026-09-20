package com.example.lld.warehouse_fulfilment_domain.model;

public enum FulfilmentStatus {
    RECEIVED,
    ALLOCATED,
    PICKING,
    PICKED,
    PACKED,
    PARTIALLY_SHIPPED,
    SHIPPED,
    CANCELLED,
    EXCEPTION
}
