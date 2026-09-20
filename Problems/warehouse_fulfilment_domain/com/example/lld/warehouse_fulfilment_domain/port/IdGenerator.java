package com.example.lld.warehouse_fulfilment_domain.port;

@FunctionalInterface
public interface IdGenerator {
    String nextId(String prefix);
}
