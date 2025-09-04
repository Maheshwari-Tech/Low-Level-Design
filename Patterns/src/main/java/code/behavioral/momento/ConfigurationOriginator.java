package code.behavioral.momento;

public class ConfigurationOriginator {
    State state;
    ConfigurationCareTaker careTaker;

    ConfigurationOriginator(State state, ConfigurationCareTaker careTaker){
       this.state = state;
       this.careTaker = careTaker;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ConfigurationMomento createMomento(){
        ConfigurationMomento momento = new ConfigurationMomento(state);
        careTaker.addMomento(momento);
        return momento;
    }

    public void restore(ConfigurationMomento momento){
        this.state = momento.getState();
    }


    public static class ConfigurationMomento {
        State state;
        ConfigurationMomento(State state){
            this.state = state;
        }
        void setState(State state){
            this.state = state;
        }

        State getState(){
            return state;
        }
    }

}
