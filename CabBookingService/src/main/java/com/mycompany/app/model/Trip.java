package com.mycompany.app.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Trip {
    private static volatile AtomicInteger counter = new AtomicInteger(0);
    private final int id;
    private final int riderId;
    private final int cabId;
    private final Location from;
    private final Location to;
    private final LocalDateTime timestamp;

    public Trip(int riderId, int cabId, Location from, Location to){
        this.id = counter.getAndIncrement();
        this.cabId = cabId;
        this.from = from;
        this.to = to;
        this.riderId = riderId;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public int getCabId() {
        return cabId;
    }

    public int getRiderId() {
        return riderId;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}
