package code.creational.abstract_factory;

import java.util.Objects;

public abstract class Vehicle {
    private final String model;

    protected Vehicle(String model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public final String model() {
        return model;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[model=" + model + "]";
    }
}
