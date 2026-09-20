// FIXED: Following Interface Segregation Principle
// Separate interfaces for different responsibilities

interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

interface Codeable {
    void code();
}

interface Manageable {
    void manage();
}

interface Cleanable {
    void clean();
}

// Robot only implements what it needs
class RobotWorker implements Workable, Codeable, Cleanable {
    @Override
    public void work() { System.out.println("Robot working"); }

    @Override
    public void code() { System.out.println("Robot coding"); }

    @Override
    public void clean() { System.out.println("Robot cleaning"); }
}

// Human implements what it needs
class HumanWorker implements Workable, Eatable, Sleepable, Codeable, Manageable {
    @Override
    public void work() { System.out.println("Human working"); }

    @Override
    public void eat() { System.out.println("Human eating"); }

    @Override
    public void sleep() { System.out.println("Human sleeping"); }

    @Override
    public void code() { System.out.println("Human coding"); }

    @Override
    public void manage() { System.out.println("Human managing"); }
}

// Manager only needs management capabilities
class Manager implements Workable, Manageable {
    @Override
    public void work() { System.out.println("Manager working"); }

    @Override
    public void manage() { System.out.println("Manager managing team"); }
}

public class WorkerDemo {
    public static void main(String[] args) {
        Workable robot = new RobotWorker();
        Workable human = new HumanWorker();
        Workable manager = new Manager();

        // All can work without unnecessary methods
        robot.work();
        human.work();
        manager.work();
    }
}
