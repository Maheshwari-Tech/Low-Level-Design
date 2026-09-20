package com.example.lld.inventory_reservation_service.port;

import com.example.lld.inventory_reservation_service.model.InventoryBalance;
import com.example.lld.inventory_reservation_service.model.ReservationLine;

import java.util.List;
import java.util.Map;

/** Pure policy: choose a complete allocation from immutable balance snapshots or throw. */
@FunctionalInterface
public interface AllocationStrategy {
    List<ReservationLine> allocate(
            Map<String, Integer> requestedQuantities,
            List<InventoryBalance> balances);
}
