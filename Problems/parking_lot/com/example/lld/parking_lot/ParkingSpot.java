package com.example.lld.parking_lot;

import java.util.Optional;

public final class ParkingSpot {
    private final int no;
    private Vehicle vehicle;

    public ParkingSpot(int no) {
        if (no <= 0) {
            throw new IllegalArgumentException("Parking spot number must be positive");
        }
        this.no = no;
    }

    public void assignCar(Vehicle vehicle) {
        if (isOccupied()) {
            throw new IllegalStateException("Parking spot " + no + " is already occupied");
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }
        this.vehicle = vehicle;
    }

    public Vehicle makeFree() {
        if (!isOccupied()) {
            throw new IllegalStateException("Parking spot " + no + " is already free");
        }
        Vehicle parkedVehicle = vehicle;
        vehicle = null;
        return parkedVehicle;
    }

    public boolean isOccupied() {
        return vehicle != null;
    }

    public int getNo() {
        return no;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Optional<Vehicle> vehicle() {
        return Optional.ofNullable(vehicle);
    }
}
