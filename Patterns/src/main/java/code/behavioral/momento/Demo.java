package code.behavioral.momento;

public class Demo {
    public static void main(String[] args) {
        ConfigurationCareTaker careTaker = new ConfigurationCareTaker();

        ConfigurationOriginator originator = new ConfigurationOriginator(new State(10,20), careTaker);
        ConfigurationOriginator.ConfigurationMomento snapshot1 = originator.createMomento();

        originator.setState(new State(20, 30));

        ConfigurationOriginator.ConfigurationMomento snapshot2 = originator.createMomento();

        ConfigurationOriginator.ConfigurationMomento old = careTaker.undo();
        System.out.println(originator.state.height + " " + originator.state.width);

        originator.restore(old);

        System.out.println(originator.state.height + " " + originator.state.width);



    }
}
