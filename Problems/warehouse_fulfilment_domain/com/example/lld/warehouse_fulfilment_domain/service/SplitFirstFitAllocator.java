package com.example.lld.warehouse_fulfilment_domain.service;

import com.example.lld.warehouse_fulfilment_domain.exception.AllocationException;
import com.example.lld.warehouse_fulfilment_domain.model.Allocation;
import com.example.lld.warehouse_fulfilment_domain.model.OrderLineDemand;
import com.example.lld.warehouse_fulfilment_domain.model.WarehouseAvailability;
import com.example.lld.warehouse_fulfilment_domain.port.AllocationStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic first-fit that may split one line across warehouses and bins. */
public final class SplitFirstFitAllocator implements AllocationStrategy {
    @Override
    public List<Allocation> allocate(
            List<OrderLineDemand> demand,
            List<WarehouseAvailability> availability) {
        List<Slot> slots = availability.stream()
                .sorted(Comparator.comparing(WarehouseAvailability::warehouseId)
                        .thenComparing(WarehouseAvailability::binId)
                        .thenComparing(WarehouseAvailability::sku))
                .map(Slot::new)
                .toList();
        List<Allocation> result = new ArrayList<>();
        for (OrderLineDemand line : demand) {
            int remaining = line.quantity();
            for (Slot slot : slots) {
                if (!slot.availability.sku().equals(line.sku()) || slot.remaining == 0) {
                    continue;
                }
                int quantity = Math.min(remaining, slot.remaining);
                result.add(new Allocation(
                        line.orderLineId(), line.sku(), slot.availability.warehouseId(),
                        slot.availability.binId(), quantity));
                slot.remaining -= quantity;
                remaining -= quantity;
                if (remaining == 0) {
                    break;
                }
            }
            if (remaining > 0) {
                throw new AllocationException(
                        "Unable to allocate " + remaining + " remaining unit(s) for line "
                                + line.orderLineId() + " / SKU " + line.sku());
            }
        }
        return List.copyOf(result);
    }

    private static final class Slot {
        private final WarehouseAvailability availability;
        private int remaining;

        private Slot(WarehouseAvailability availability) {
            this.availability = availability;
            this.remaining = availability.availableQuantity();
        }
    }
}
