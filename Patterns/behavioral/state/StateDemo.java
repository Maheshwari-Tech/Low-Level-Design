package code.behavioral.state;

public final class StateDemo {
    private StateDemo() {
    }

    private sealed interface TrafficLightState permits Red, Green, Amber {
        String color();

        TrafficLightState next();
    }

    private enum Red implements TrafficLightState {
        INSTANCE;

        @Override
        public String color() {
            return "RED";
        }

        @Override
        public TrafficLightState next() {
            return Green.INSTANCE;
        }
    }

    private enum Green implements TrafficLightState {
        INSTANCE;

        @Override
        public String color() {
            return "GREEN";
        }

        @Override
        public TrafficLightState next() {
            return Amber.INSTANCE;
        }
    }

    private enum Amber implements TrafficLightState {
        INSTANCE;

        @Override
        public String color() {
            return "AMBER";
        }

        @Override
        public TrafficLightState next() {
            return Red.INSTANCE;
        }
    }

    private static final class TrafficLight {
        private TrafficLightState state = Red.INSTANCE;

        void advance() {
            System.out.println(state.color());
            state = state.next();
        }
    }

    public static void main(String[] args) {
        TrafficLight light = new TrafficLight();
        for (int transition = 0; transition < 4; transition++) {
            light.advance();
        }
    }
}
