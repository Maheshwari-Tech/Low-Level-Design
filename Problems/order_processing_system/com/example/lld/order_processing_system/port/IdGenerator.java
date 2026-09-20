package com.example.lld.order_processing_system.port;

@FunctionalInterface
public interface IdGenerator {
    String nextId(String prefix);
}
