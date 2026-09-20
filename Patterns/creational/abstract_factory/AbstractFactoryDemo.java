package code.creational.abstract_factory;

public final class AbstractFactoryDemo {
    private AbstractFactoryDemo() {
    }

    public static void main(String[] args) {
        for (vechicleFactoryFactory.Segment segment : vechicleFactoryFactory.Segment.values()) {
            VehicleFactory factory = vechicleFactoryFactory.forSegment(segment);
            System.out.printf("%s -> %s with %s%n",
                    segment, factory.getVehicle(), factory.getMaintenancePlan());
        }
    }
}
