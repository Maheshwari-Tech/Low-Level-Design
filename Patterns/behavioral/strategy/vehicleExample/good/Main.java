package code.behavioral.strategy.vehicleExample.good;

import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        List<Vehicle> vehicles = List.of(new NormalVehicle(), new OffRoadVechile(), new SportsVechile());
        vehicles.forEach(Vehicle::driving);
    }
}
