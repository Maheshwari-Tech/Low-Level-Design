package code.structural.bridge;

public final class Dog extends LivingThing {
    public Dog(BreathingProcess implementation) {
        super(implementation);
    }

    @Override
    public void breathingProcess() {
        breatheUsing("Dog");
    }
}
