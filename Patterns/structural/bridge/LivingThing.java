package code.structural.bridge;

import java.util.Objects;

public abstract class LivingThing {
    private final BreathingProcess implementation;

    protected LivingThing(BreathingProcess implementation) {
        this.implementation = Objects.requireNonNull(implementation, "implementation");
    }

    protected final void breatheUsing(String owner) {
        implementation.breathe(owner);
    }

    public abstract void breathingProcess();
}
