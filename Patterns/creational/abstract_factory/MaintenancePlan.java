package code.creational.abstract_factory;

import java.util.Objects;

public record MaintenancePlan(String name, int serviceIntervalKm) {
    public MaintenancePlan {
        Objects.requireNonNull(name, "name");
        if (serviceIntervalKm <= 0) {
            throw new IllegalArgumentException("service interval must be positive");
        }
    }
}
