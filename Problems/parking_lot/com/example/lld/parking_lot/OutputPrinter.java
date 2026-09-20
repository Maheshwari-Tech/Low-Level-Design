package com.example.lld.parking_lot;

import java.io.PrintStream;
import java.util.List;

public final class OutputPrinter {
    private final PrintStream output;

    public OutputPrinter() {
        this(System.out);
    }

    public OutputPrinter(PrintStream output) {
        this.output = output;
    }

    public void parkingLotCreated(int capacity) {
        output.println("Created a parking lot with " + capacity + " slots");
    }

    public void allocated(int slotId) {
        output.println("Allocated slot number: " + slotId);
    }

    public void freed(int slotId) {
        output.println("Slot number " + slotId + " is free");
    }

    public void status(List<ParkingSpot> occupiedSpots) {
        output.println("Slot No. Registration No Colour");
        occupiedSpots.forEach(spot -> output.printf(
                "%d %s %s%n",
                spot.getNo(),
                spot.getVehicle().no(),
                spot.getVehicle().color()));
    }
}
