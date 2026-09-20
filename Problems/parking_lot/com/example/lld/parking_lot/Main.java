package com.example.lld.parking_lot;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ParkingLotService service = new ParkingLotService();
        OutputPrinter printer = new OutputPrinter();

        service.createParkingLot(3, new NaturalNumberStrategy());
        printer.parkingLotCreated(3);

        int firstSlot = service.parkVehicle(new Vehicle("KA-01-HH-1234", "White"));
        printer.allocated(firstSlot);
        printer.allocated(service.parkVehicle(new Vehicle("KA-01-HH-9999", "Black")));
        printer.status(service.status());

        System.out.println("White registrations: " + service.carNumbersWithColor("white"));
        service.unparkVehicle(firstSlot);
        printer.freed(firstSlot);
    }
}
