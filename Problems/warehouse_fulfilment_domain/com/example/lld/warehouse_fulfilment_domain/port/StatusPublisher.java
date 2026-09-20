package com.example.lld.warehouse_fulfilment_domain.port;

import com.example.lld.warehouse_fulfilment_domain.model.FulfilmentEvent;

/** Upstream integration must deduplicate by eventId. */
@FunctionalInterface
public interface StatusPublisher {
    void publish(FulfilmentEvent event);
}
