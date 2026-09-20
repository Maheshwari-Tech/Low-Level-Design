package code.behavioral.momento;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationCareTaker {
    private final List<ConfigurationOriginator.ConfigurationMomento> history = new ArrayList<>();

    public void addMomento(ConfigurationOriginator.ConfigurationMomento obj) {
        history.add(obj);
    }

    public ConfigurationOriginator.ConfigurationMomento undo() {
        if (history.size() < 2) {
            throw new IllegalStateException("no earlier configuration");
        }
        history.remove(history.size() - 1);
        return history.get(history.size() - 1);
    }
}
