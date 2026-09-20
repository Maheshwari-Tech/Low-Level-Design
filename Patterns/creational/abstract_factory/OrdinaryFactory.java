package code.creational.abstract_factory;

public final class OrdinaryFactory implements VehicleFactory {
    @Override
    public Vehicle getVehicle() {
        return new Swift();
    }

    @Override
    public MaintenancePlan getMaintenancePlan() {
        return new MaintenancePlan("standard care", 10_000);
    }
}
