package com.example.lld.warehouse_fulfilment_domain.port;

import com.example.lld.warehouse_fulfilment_domain.model.Allocation;
import com.example.lld.warehouse_fulfilment_domain.model.OrderLineDemand;
import com.example.lld.warehouse_fulfilment_domain.model.WarehouseAvailability;

import java.util.List;

/** Pure allocation policy: return a complete allocation or throw without side effects. */
@FunctionalInterface
public interface AllocationStrategy {
    List<Allocation> allocate(
            List<OrderLineDemand> demand,
            List<WarehouseAvailability> availability);
}
