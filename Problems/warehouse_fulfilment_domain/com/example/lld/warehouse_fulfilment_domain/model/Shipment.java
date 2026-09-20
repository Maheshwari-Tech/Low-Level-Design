package com.example.lld.warehouse_fulfilment_domain.model;

import java.time.Instant;

public record Shipment(
        String shipmentId,
        String fulfilmentId,
        String packageId,
        String carrier,
        String trackingNumber,
        State state,
        Instant shippedAt) {

    public enum State {
        SHIPPED
    }

    public Shipment {
        if (shipmentId == null || shipmentId.isBlank()
                || fulfilmentId == null || fulfilmentId.isBlank()
                || packageId == null || packageId.isBlank()
                || carrier == null || carrier.isBlank()
                || trackingNumber == null || trackingNumber.isBlank()
                || state == null || shippedAt == null) {
            throw new IllegalArgumentException("complete shipment identity and tracking are required");
        }
    }
}
