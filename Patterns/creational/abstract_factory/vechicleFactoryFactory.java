package code.creational.abstract_factory;

public final class vechicleFactoryFactory {
    public enum Segment {
        ORDINARY,
        LUXURY
    }

    private vechicleFactoryFactory() {
    }

    public static VehicleFactory forSegment(Segment segment) {
        return switch (segment) {
            case ORDINARY -> new OrdinaryFactory();
            case LUXURY -> new LuxaryFactory();
        };
    }
}
