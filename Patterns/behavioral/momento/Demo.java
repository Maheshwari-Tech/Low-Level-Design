package code.behavioral.momento;

public class Demo {
    public static void main(String[] args) {
        ConfigurationCareTaker careTaker = new ConfigurationCareTaker();

        ConfigurationOriginator originator = new ConfigurationOriginator(new State(10, 20), careTaker);
        originator.createMomento();

        originator.setState(new State(20, 30));
        originator.createMomento();
        System.out.println("current:  " + originator.currentState());

        originator.restore(careTaker.undo());
        System.out.println("restored: " + originator.currentState());
    }
}
