package code.structural.bridge;

public final class Tree extends LivingThing {
    public Tree(BreathingProcess implementation) {
        super(implementation);
    }

    @Override
    public void breathingProcess() {
        breatheUsing("Tree");
    }
}
