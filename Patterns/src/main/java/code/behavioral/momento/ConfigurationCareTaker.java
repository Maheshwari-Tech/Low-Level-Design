package code.behavioral.momento;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationCareTaker {
    List<ConfigurationOriginator.ConfigurationMomento> history = new ArrayList<>();

    public void addMomento(ConfigurationOriginator.ConfigurationMomento obj){
        history.add(obj);
    }

    public ConfigurationOriginator.ConfigurationMomento undo(){
        return history.remove(history.size()-1);
    }
}
