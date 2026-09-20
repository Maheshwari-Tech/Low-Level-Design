package com.example.lld.elevator_system;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ElevatorSystem system = new ElevatorSystem(3, 10);
        system.requestElevator(5, Direction.UP);
        system.selectFloor(1, 8);
        system.requestElevator(3, Direction.DOWN);
        system.displayStatus();
    }
}
