package code.behavioral.visitor.good;

import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        List<RoomElement> rooms = List.of(
                new SingleRoom(100),
                new DoubleRoom(160),
                new DeluxRoom(250));

        List<RoomVisitor> operations = List.of(
                new RoomPricingVisitor(),
                new RoomBookingVisitor(),
                new RoomMaintenanceVisitor());

        operations.forEach(visitor -> rooms.forEach(room -> room.accept(visitor)));
    }
}
