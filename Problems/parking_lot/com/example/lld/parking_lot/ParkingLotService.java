package com.example.lld.parking_lot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ParkingLotService {
    private final Map<String, Integer> slotByVehicleNo = new LinkedHashMap<>();
    private final Map<String, Set<String>> vehicleNumbersByColor = new LinkedHashMap<>();

    private ParkingLot parkingLot;
    private ParkingStrategy parkingStrategy;

    public void createParkingLot(int capacity, ParkingStrategy strategy) {
        parkingLot = new ParkingLot(capacity);
        parkingStrategy = Objects.requireNonNull(strategy, "strategy");
        parkingStrategy.reset();
        slotByVehicleNo.clear();
        vehicleNumbersByColor.clear();

        for (int slot = 1; slot <= capacity; slot++) {
            parkingStrategy.addId(slot);
        }
    }

    public int parkVehicle(Vehicle vehicle) {
        ensureCreated();
        Vehicle requiredVehicle = Objects.requireNonNull(vehicle, "vehicle");
        if (slotByVehicleNo.containsKey(requiredVehicle.no())) {
            throw new IllegalArgumentException("Vehicle is already parked: " + requiredVehicle.no());
        }

        int slotId = parkingStrategy.getNextSlot();
        parkingLot.park(slotId, requiredVehicle);
        parkingStrategy.removeId(slotId);
        slotByVehicleNo.put(requiredVehicle.no(), slotId);
        vehicleNumbersByColor
                .computeIfAbsent(normalizeColor(requiredVehicle.color()), ignored -> new LinkedHashSet<>())
                .add(requiredVehicle.no());
        return slotId;
    }

    public Vehicle unparkVehicle(String vehicleNo) {
        ensureCreated();
        Integer slotId = slotByVehicleNo.get(vehicleNo);
        if (slotId == null) {
            throw new IllegalArgumentException("Vehicle is not parked: " + vehicleNo);
        }
        return unparkVehicle(slotId);
    }

    public Vehicle unparkVehicle(int slotId) {
        ensureCreated();
        Vehicle vehicle = parkingLot.unpark(slotId);
        parkingStrategy.addId(slotId);
        slotByVehicleNo.remove(vehicle.no());

        String colorKey = normalizeColor(vehicle.color());
        Set<String> registrations = vehicleNumbersByColor.get(colorKey);
        if (registrations != null) {
            registrations.remove(vehicle.no());
            if (registrations.isEmpty()) {
                vehicleNumbersByColor.remove(colorKey);
            }
        }
        return vehicle;
    }

    public List<ParkingSpot> status() {
        ensureCreated();
        return parkingLot.getSpots().values().stream()
                .filter(ParkingSpot::isOccupied)
                .sorted(Comparator.comparingInt(ParkingSpot::getNo))
                .toList();
    }

    public int getSlotNoByVehicleNo(Vehicle vehicle) {
        return getSlotNoByVehicleNo(vehicle.no());
    }

    public int getSlotNoByVehicleNo(String vehicleNo) {
        ensureCreated();
        return slotByVehicleNo.getOrDefault(vehicleNo, -1);
    }

    public List<Integer> slotsNoOfCarWithColor(String color) {
        List<Integer> slots = new ArrayList<>();
        for (String vehicleNo : registrationsForColor(color)) {
            slots.add(slotByVehicleNo.get(vehicleNo));
        }
        slots.sort(Integer::compareTo);
        return List.copyOf(slots);
    }

    public List<String> carNumbersWithColor(String color) {
        return List.copyOf(registrationsForColor(color));
    }

    public ParkingLot getParkingLot() {
        ensureCreated();
        return parkingLot;
    }

    private Set<String> registrationsForColor(String color) {
        ensureCreated();
        return vehicleNumbersByColor.getOrDefault(normalizeColor(color), Set.of());
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Color is required");
        }
        return color.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureCreated() {
        if (parkingLot == null || parkingStrategy == null) {
            throw new IllegalStateException("Create the parking lot before using it");
        }
    }
}
