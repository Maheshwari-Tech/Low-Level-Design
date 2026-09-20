package code.behavioral.momento;

import java.util.Objects;

public class ConfigurationOriginator {
    private State state;
    private final ConfigurationCareTaker careTaker;

    ConfigurationOriginator(State state, ConfigurationCareTaker careTaker) {
        this.state = Objects.requireNonNull(state, "state");
        this.careTaker = Objects.requireNonNull(careTaker, "careTaker");
    }

    public void setState(State state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public State currentState() {
        return state;
    }

    public ConfigurationMomento createMomento() {
        ConfigurationMomento momento = new ConfigurationMomento(state);
        careTaker.addMomento(momento);
        return momento;
    }

    public void restore(ConfigurationMomento momento) {
        this.state = momento.getState();
    }

    public static final class ConfigurationMomento {
        private final State state;

        private ConfigurationMomento(State state) {
            this.state = state;
        }

        private State getState() {
            return state;
        }
    }
}
