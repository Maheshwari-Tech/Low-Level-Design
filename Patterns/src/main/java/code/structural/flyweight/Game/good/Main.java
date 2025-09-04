package code.structural.flyweight.Game.good;

public class Main {
    public static void main(String[] args) {
        Robot h1 = RoboticFactory.createRobot("human");
        h1.display(12, 21);

        Robot d1 = RoboticFactory.createRobot("dog");
        d1.display(10, 20);

        Robot h2 = RoboticFactory.createRobot("human");
        h1.display(15, 0);

        Robot d2 = RoboticFactory.createRobot("dog");
        d1.display(5, 10);
    }
}
