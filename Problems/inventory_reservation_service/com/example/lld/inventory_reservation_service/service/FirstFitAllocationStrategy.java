package com.example.lld.inventory_reservation_service.service;

import com.example.lld.inventory_reservation_service.exception.InsufficientInventoryException;
import com.example.lld.inventory_reservation_service.model.InventoryBalance;
import com.example.lld.inventory_reservation_service.model.ReservationLine;
import com.example.lld.inventory_reservation_service.port.AllocationStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Deterministic SKU/location first-fit policy; it never mutates service state. */
public final class FirstFitAllocationStrategy implements AllocationStrategy {
    @Override
    public List<ReservationLine> allocate(
            Map<String, Integer> requestedQuantities,
            List<InventoryBalance> balances) {
        List<InventoryBalance> orderedBalances = balances.stream()
                .sorted(Comparator.comparing(InventoryBalance::key))
                .toList();
        List<ReservationLine> result = new ArrayList<>();
        for (Map.Entry<String, Integer> request
                : new TreeMap<>(requestedQuantities).entrySet()) {
            int remaining = request.getValue();
            int totalAvailable = 0;
            for (InventoryBalance balance : orderedBalances) {
                if (!balance.key().sku().equals(request.getKey())) {
                    continue;
                }
                totalAvailable = Math.addExact(totalAvailable, balance.available());
                int allocated = Math.min(remaining, balance.available());
                if (allocated > 0) {
                    result.add(new ReservationLine(
                            request.getKey(), balance.key().location(), allocated));
                    remaining -= allocated;
                }
                if (remaining == 0) {
                    break;
                }
            }
            if (remaining > 0) {
                throw new InsufficientInventoryException(
                        request.getKey(), request.getValue(), totalAvailable);
            }
        }
        return List.copyOf(result);
    }
}
