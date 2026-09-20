package code.structural.bridge;

public final class BreathingProcesses {
    private BreathingProcesses() {
    }

    public static final class Lungs implements BreathingProcess {
        @Override
        public void breathe(String owner) {
            System.out.println(owner + " breathes air with lungs");
        }
    }

    public static final class Gills implements BreathingProcess {
        @Override
        public void breathe(String owner) {
            System.out.println(owner + " extracts oxygen with gills");
        }
    }

    public static final class Stomata implements BreathingProcess {
        @Override
        public void breathe(String owner) {
            System.out.println(owner + " exchanges gases through stomata");
        }
    }
}
