package com.example.lld.warehouse_fulfilment_domain.port;

/** Idempotent carrier boundary; no real external call appears in this reference solution. */
@FunctionalInterface
public interface CarrierPort {
    CarrierConfirmation handOver(
            String packageId,
            String requestedCarrier,
            String requestedTrackingReference,
            String idempotencyKey);

    record CarrierConfirmation(String carrier, String trackingNumber) {
        public CarrierConfirmation {
            if (carrier == null || carrier.isBlank()
                    || trackingNumber == null || trackingNumber.isBlank()) {
                throw new IllegalArgumentException("carrier and trackingNumber must not be blank");
            }
        }
    }
}
