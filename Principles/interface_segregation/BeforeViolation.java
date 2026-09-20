// VIOLATION: Interface Segregation Principle
// Fat interface that forces implementations to implement unnecessary methods

interface Worker {
    void work();
    void eat();
    void sleep();
    void code();
    void manage();
    void clean();
}

// RobotWorker doesn't need eat() or sleep()
class RobotWorker implements Worker {
    @Override
    public void work() { System.out.println("Robot working"); }

    @Override
    public void eat() { /* Not applicable */ }

    @Override
    public void sleep() { /* Not applicable */ }

    @Override
    public void code() { System.out.println("Robot coding"); }

    @Override
    public void manage() { /* Not applicable */ }

    @Override
    public void clean() { System.out.println("Robot cleaning"); }
}

// HumanWorker doesn't need clean() or some management tasks
class HumanWorker implements Worker {
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

    @Override
    public void clean() { /* Not applicable */ }
}
