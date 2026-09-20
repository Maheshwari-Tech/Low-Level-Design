package com.example.lld.inventory_reservation_service.port;

@FunctionalInterface
public interface IdGenerator {
    String nextId();
}
