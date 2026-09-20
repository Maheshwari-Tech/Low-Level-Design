package com.example.lld.warehouse_fulfilment_domain.model;

import com.example.lld.warehouse_fulfilment_domain.exception.InvalidScanException;
import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;

import java.util.ArrayList;
import java.util.List;

/** Immutable physical package and exact order-line contents. */
public record FulfilmentPackage(
        String packageId,
        String fulfilmentId,
        String warehouseId,
        Status status,
        List<PackageContent> contents,
        int weightGrams,
        String shipmentId) {

    public enum Status {
        OPEN,
        SEALED,
        SHIPPED,
        CANCELLED
    }

    public FulfilmentPackage {
        if (packageId == null || packageId.isBlank()
                || fulfilmentId == null || fulfilmentId.isBlank()
                || warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("package identifiers must not be blank");
        }
        if (status == null || weightGrams < 0) {
            throw new IllegalArgumentException("valid package status and weight are required");
        }
        contents = List.copyOf(contents);
        if (status == Status.OPEN && (weightGrams != 0 || shipmentId != null)) {
            throw new IllegalArgumentException("open package cannot have weight or shipment");
        }
        if ((status == Status.SEALED || status == Status.SHIPPED)
                && (contents.isEmpty() || weightGrams <= 0)) {
            throw new IllegalArgumentException("sealed/shipped package needs contents and weight");
        }
        if (status == Status.SHIPPED && (shipmentId == null || shipmentId.isBlank())) {
            throw new IllegalArgumentException("shipped package requires shipmentId");
        }
    }

    public static FulfilmentPackage open(
            String packageId, String fulfilmentId, String warehouseId) {
        return new FulfilmentPackage(
                packageId, fulfilmentId, warehouseId, Status.OPEN, List.of(), 0, null);
    }

    public FulfilmentPackage addContent(String orderLineId, String sku, int quantity) {
        if (status != Status.OPEN) {
            throw new InvalidScanException("Package " + packageId + " is not open");
        }
        if (quantity <= 0) {
            throw new InvalidScanException("Package scan quantity must be positive");
        }
        List<PackageContent> updated = new ArrayList<>();
        boolean merged = false;
        for (PackageContent content : contents) {
            if (content.orderLineId().equals(orderLineId)) {
                if (!content.sku().equals(sku)) {
                    throw new InvalidScanException("SKU does not match the package line");
                }
                updated.add(new PackageContent(
                        orderLineId, sku, Math.addExact(content.quantity(), quantity)));
                merged = true;
            } else {
                updated.add(content);
            }
        }
        if (!merged) {
            updated.add(new PackageContent(orderLineId, sku, quantity));
        }
        return new FulfilmentPackage(
                packageId, fulfilmentId, warehouseId, status, updated, 0, null);
    }

    public FulfilmentPackage seal(int finalWeightGrams) {
        if (status != Status.OPEN || contents.isEmpty() || finalWeightGrams <= 0) {
            throw new InvalidStateException(
                    "Package " + packageId + " must be non-empty and open before sealing");
        }
        return new FulfilmentPackage(
                packageId, fulfilmentId, warehouseId, Status.SEALED, contents,
                finalWeightGrams, null);
    }

    public FulfilmentPackage markShipped(String confirmedShipmentId) {
        if (status != Status.SEALED) {
            throw new InvalidStateException(
                    "Package " + packageId + " cannot ship from " + status);
        }
        return new FulfilmentPackage(
                packageId, fulfilmentId, warehouseId, Status.SHIPPED, contents,
                weightGrams, confirmedShipmentId);
    }

    public FulfilmentPackage cancelEmpty() {
        if (status != Status.OPEN || !contents.isEmpty()) {
            throw new InvalidStateException("Only an empty open package can be cancelled");
        }
        return new FulfilmentPackage(
                packageId, fulfilmentId, warehouseId, Status.CANCELLED, contents, 0, null);
    }
}
