package code.structural.bridge;

public final class Fish extends LivingThing {
    public Fish(BreathingProcess implementation) {
        super(implementation);
    }

    @Override
    public void breathingProcess() {
        breatheUsing("Fish");
    }
}
