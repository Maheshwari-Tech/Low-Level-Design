package com.example.lld.elevator_system;

import java.util.ArrayDeque;
import java.util.Queue;

public final class Floor {
    private final int floorNumber;
    private final Queue<Request> upRequests = new ArrayDeque<>();
    private final Queue<Request> downRequests = new ArrayDeque<>();

    public Floor(int floorNumber) {
        if (floorNumber < 0) {
            throw new IllegalArgumentException("Floor cannot be negative");
        }
        this.floorNumber = floorNumber;
    }

    public void addUpRequest() {
        addRequest(Direction.UP);
    }

    public void addDownRequest() {
        addRequest(Direction.DOWN);
    }

    public void addRequest(Direction direction) {
        Request request = new Request(floorNumber, direction);
        queue(direction).add(request);
        System.out.printf("Floor %d: %s request added%n", floorNumber, direction);
    }

    public Request getNextUpRequest() {
        return upRequests.poll();
    }

    public Request getNextDownRequest() {
        return downRequests.poll();
    }

    public boolean hasUpRequests() {
        return !upRequests.isEmpty();
    }

    public boolean hasDownRequests() {
        return !downRequests.isEmpty();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    void markServed(Direction direction) {
        queue(direction).poll();
    }

    private Queue<Request> queue(Direction direction) {
        return switch (direction) {
            case UP -> upRequests;
            case DOWN -> downRequests;
            case IDLE -> throw new IllegalArgumentException("IDLE is not a request direction");
        };
    }
}
