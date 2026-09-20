package com.example.lld.warehouse_fulfilment_domain.model;

import com.example.lld.warehouse_fulfilment_domain.exception.InvalidStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable aggregate summary; task, package, and shipment IDs point to immutable snapshots. */
public final class FulfilmentOrder {
    private final String fulfilmentId;
    private final String externalOrderId;
    private final Destination destination;
    private final Instant createdAt;
    private final FulfilmentStatus status;
    private final List<FulfilmentLine> lines;
    private final List<String> pickTaskIds;
    private final List<String> packageIds;
    private final List<String> shipmentIds;
    private final List<AuditEntry> auditTrail;

    private FulfilmentOrder(
            String fulfilmentId,
            String externalOrderId,
            Destination destination,
            Instant createdAt,
            FulfilmentStatus status,
            List<FulfilmentLine> lines,
            List<String> pickTaskIds,
            List<String> packageIds,
            List<String> shipmentIds,
            List<AuditEntry> auditTrail) {
        this.fulfilmentId = requireText(fulfilmentId, "fulfilmentId");
        this.externalOrderId = requireText(externalOrderId, "externalOrderId");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
        this.lines = List.copyOf(lines);
        this.pickTaskIds = distinctCopy(pickTaskIds, "pick task");
        this.packageIds = distinctCopy(packageIds, "package");
        this.shipmentIds = distinctCopy(shipmentIds, "shipment");
        this.auditTrail = List.copyOf(auditTrail);
        if (this.lines.isEmpty()) {
            throw new IllegalArgumentException("a fulfilment order requires lines");
        }
        Set<String> lineIds = new HashSet<>();
        if (this.lines.stream().anyMatch(line -> !lineIds.add(line.orderLineId()))) {
            throw new IllegalArgumentException("order-line IDs must be unique");
        }
    }

    public static FulfilmentOrder received(
            String fulfilmentId,
            String externalOrderId,
            Destination destination,
            List<FulfilmentLine> lines,
            Instant createdAt,
            AuditEntry audit) {
        return new FulfilmentOrder(
                fulfilmentId, externalOrderId, destination, createdAt,
                FulfilmentStatus.RECEIVED, lines, List.of(), List.of(), List.of(),
                List.of(audit));
    }

    public FulfilmentOrder addTasks(
            List<String> newTaskIds,
            List<FulfilmentLine> updatedLines,
            FulfilmentStatus nextStatus,
            AuditEntry audit) {
        List<String> tasks = new ArrayList<>(pickTaskIds);
        tasks.addAll(newTaskIds);
        return evolve(nextStatus, updatedLines, tasks, packageIds, shipmentIds, audit);
    }

    public FulfilmentOrder updateLine(
            FulfilmentLine updatedLine,
            FulfilmentStatus nextStatus,
            AuditEntry audit) {
        List<FulfilmentLine> updated = new ArrayList<>(lines.size());
        boolean found = false;
        for (FulfilmentLine line : lines) {
            if (line.orderLineId().equals(updatedLine.orderLineId())) {
                updated.add(updatedLine);
                found = true;
            } else {
                updated.add(line);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown order line: " + updatedLine.orderLineId());
        }
        return evolve(nextStatus, updated, pickTaskIds, packageIds, shipmentIds, audit);
    }

    public FulfilmentOrder updateLines(
            List<FulfilmentLine> updatedLines,
            FulfilmentStatus nextStatus,
            AuditEntry audit) {
        return evolve(nextStatus, updatedLines, pickTaskIds, packageIds, shipmentIds, audit);
    }

    public FulfilmentOrder addPackage(String packageId, AuditEntry audit) {
        List<String> packages = new ArrayList<>(packageIds);
        packages.add(packageId);
        return evolve(status, lines, pickTaskIds, packages, shipmentIds, audit);
    }

    public FulfilmentOrder addShipment(
            String shipmentId,
            List<FulfilmentLine> updatedLines,
            FulfilmentStatus nextStatus,
            AuditEntry audit) {
        List<String> shipments = new ArrayList<>(shipmentIds);
        shipments.add(shipmentId);
        return evolve(nextStatus, updatedLines, pickTaskIds, packageIds, shipments, audit);
    }

    public FulfilmentOrder transition(FulfilmentStatus nextStatus, AuditEntry audit) {
        return evolve(nextStatus, lines, pickTaskIds, packageIds, shipmentIds, audit);
    }

    public String fulfilmentId() {
        return fulfilmentId;
    }

    public String externalOrderId() {
        return externalOrderId;
    }

    public Destination destination() {
        return destination;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public FulfilmentStatus status() {
        return status;
    }

    public List<FulfilmentLine> lines() {
        return lines;
    }

    public List<String> pickTaskIds() {
        return pickTaskIds;
    }

    public List<String> packageIds() {
        return packageIds;
    }

    public List<String> shipmentIds() {
        return shipmentIds;
    }

    public List<AuditEntry> auditTrail() {
        return auditTrail;
    }

    public FulfilmentLine line(String orderLineId) {
        return lines.stream()
                .filter(line -> line.orderLineId().equals(orderLineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown order line: " + orderLineId));
    }

    private FulfilmentOrder evolve(
            FulfilmentStatus nextStatus,
            List<FulfilmentLine> nextLines,
            List<String> nextTaskIds,
            List<String> nextPackageIds,
            List<String> nextShipmentIds,
            AuditEntry audit) {
        if (!transitionAllowed(status, nextStatus)) {
            throw new InvalidStateException(
                    "Fulfilment " + fulfilmentId + " cannot transition from " + status
                            + " to " + nextStatus);
        }
        List<AuditEntry> history = new ArrayList<>(auditTrail);
        history.add(audit);
        return new FulfilmentOrder(
                fulfilmentId, externalOrderId, destination, createdAt, nextStatus,
                nextLines, nextTaskIds, nextPackageIds, nextShipmentIds, history);
    }

    private static boolean transitionAllowed(
            FulfilmentStatus from, FulfilmentStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case RECEIVED -> to == FulfilmentStatus.ALLOCATED
                    || to == FulfilmentStatus.CANCELLED
                    || to == FulfilmentStatus.EXCEPTION;
            case ALLOCATED -> to == FulfilmentStatus.PICKING
                    || to == FulfilmentStatus.PICKED
                    || to == FulfilmentStatus.CANCELLED
                    || to == FulfilmentStatus.EXCEPTION;
            case PICKING -> to == FulfilmentStatus.ALLOCATED
                    || to == FulfilmentStatus.PICKED
                    || to == FulfilmentStatus.CANCELLED
                    || to == FulfilmentStatus.EXCEPTION;
            case PICKED -> to == FulfilmentStatus.PACKED
                    || to == FulfilmentStatus.EXCEPTION;
            case PACKED -> to == FulfilmentStatus.PARTIALLY_SHIPPED
                    || to == FulfilmentStatus.SHIPPED;
            case PARTIALLY_SHIPPED -> to == FulfilmentStatus.SHIPPED;
            case EXCEPTION -> to == FulfilmentStatus.ALLOCATED
                    || to == FulfilmentStatus.PICKING
                    || to == FulfilmentStatus.CANCELLED;
            case SHIPPED, CANCELLED -> false;
        };
    }

    private static List<String> distinctCopy(List<String> values, String label) {
        List<String> result = List.copyOf(values);
        if (new HashSet<>(result).size() != result.size()) {
            throw new IllegalArgumentException(label + " IDs must be unique");
        }
        return result;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "FulfilmentOrder{" + fulfilmentId + ", externalOrder=" + externalOrderId
                + ", status=" + status + ", tasks=" + pickTaskIds.size()
                + ", packages=" + packageIds.size() + '}';
    }
}
