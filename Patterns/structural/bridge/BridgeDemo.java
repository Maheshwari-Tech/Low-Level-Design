package code.structural.bridge;

import java.util.List;

public final class BridgeDemo {
    private BridgeDemo() {
    }

    public static void main(String[] args) {
        List<LivingThing> livingThings = List.of(
                new Dog(new BreathingProcesses.Lungs()),
                new Fish(new BreathingProcesses.Gills()),
                new Tree(new BreathingProcesses.Stomata()));
        livingThings.forEach(LivingThing::breathingProcess);
    }
}
