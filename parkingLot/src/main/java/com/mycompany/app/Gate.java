package com.mycompany.app;

import com.mycompany.app.vehicles.Vehicle;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Gate {
    int id;
    AtomicInteger counter = new AtomicInteger(0);

    Gate(){
        this.id = counter.getAndIncrement();
    }

    int getId(){
        return this.id;
    }

    abstract void process(Vehicle vehicle);
}
