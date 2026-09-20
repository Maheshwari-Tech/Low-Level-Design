package com.mycompany.app.models;

import java.util.concurrent.atomic.AtomicInteger;

public class Theatre {
    private final Address address;
    private final int id;
    private String name;
    private static volatile AtomicInteger counter = new AtomicInteger();

    public Theatre(Address address, String name) {
        this.address = address;
        this.id = counter.incrementAndGet();
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
