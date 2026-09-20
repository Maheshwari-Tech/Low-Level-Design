package com.example.lld.parking_lot;

import java.util.NoSuchElementException;
import java.util.TreeSet;

/** Allocates the lowest numbered available spot. */
public final class NaturalNumberStrategy implements ParkingStrategy {
    private final TreeSet<Integer> availableIds = new TreeSet<>();

    @Override
    public void reset() {
        availableIds.clear();
    }

    @Override
    public void addId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Parking spot number must be positive");
        }
        availableIds.add(id);
    }

    @Override
    public int getNextSlot() {
        if (availableIds.isEmpty()) {
            throw new NoSuchElementException("Parking lot is full");
        }
        return availableIds.first();
    }

    @Override
    public void removeId(int id) {
        if (!availableIds.remove(id)) {
            throw new IllegalStateException("Parking spot is not available: " + id);
        }
    }
}
