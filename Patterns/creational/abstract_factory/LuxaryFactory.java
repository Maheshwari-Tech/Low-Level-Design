package code.creational.abstract_factory;

public final class LuxaryFactory implements VehicleFactory {
    @Override
    public Vehicle getVehicle() {
        return new BMW();
    }

    @Override
    public MaintenancePlan getMaintenancePlan() {
        return new MaintenancePlan("premium care", 15_000);
    }
}
