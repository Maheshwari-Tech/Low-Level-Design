package com.example.lld.parking_lot;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParkingLot {
    private final int capacity;
    private final Map<Integer, ParkingSpot> spots = new LinkedHashMap<>();

    public ParkingLot(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Parking lot capacity must be positive");
        }
        this.capacity = capacity;
        for (int slot = 1; slot <= capacity; slot++) {
            spots.put(slot, new ParkingSpot(slot));
        }
    }

    public void park(int slotId, Vehicle vehicle) {
        requireSpot(slotId).assignCar(vehicle);
    }

    public Vehicle unpark(int slotId) {
        return requireSpot(slotId).makeFree();
    }

    public int getCapacity() {
        return capacity;
    }

    public Map<Integer, ParkingSpot> getSpots() {
        return Map.copyOf(spots);
    }

    private ParkingSpot requireSpot(int slotId) {
        ParkingSpot spot = spots.get(slotId);
        if (spot == null) {
            throw new IllegalArgumentException("Unknown parking spot: " + slotId);
        }
        return spot;
    }
}
