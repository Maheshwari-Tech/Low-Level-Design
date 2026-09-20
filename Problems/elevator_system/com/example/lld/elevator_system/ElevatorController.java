package com.example.lld.elevator_system;

public final class ElevatorController {
    private final long travelMillisPerFloor;
    private final long doorTransitionMillis;

    public ElevatorController() {
        this(0, 0);
    }

    public ElevatorController(long travelMillisPerFloor, long doorTransitionMillis) {
        if (travelMillisPerFloor < 0 || doorTransitionMillis < 0) {
            throw new IllegalArgumentException("Simulation delays cannot be negative");
        }
        this.travelMillisPerFloor = travelMillisPerFloor;
        this.doorTransitionMillis = doorTransitionMillis;
    }

    public void moveElevator(Elevator elevator, int floor) {
        elevator.moveToFloor(floor);
    }

    public void openDoors(Elevator elevator) {
        elevator.transitionTo(ElevatorState.DOOR_OPENING);
        System.out.println("Elevator " + elevator.getId() + ": Doors opening...");
        pause(doorTransitionMillis);
        elevator.transitionTo(ElevatorState.DOOR_OPEN);
        System.out.println("Elevator " + elevator.getId() + ": Doors open");
    }

    public void closeDoors(Elevator elevator) {
        elevator.transitionTo(ElevatorState.DOOR_CLOSING);
        System.out.println("Elevator " + elevator.getId() + ": Doors closing...");
        pause(doorTransitionMillis);
    }

    public void emergencyStop(Elevator elevator) {
        elevator.transitionTo(ElevatorState.EMERGENCY_STOP);
        System.out.println("Elevator " + elevator.getId() + ": EMERGENCY STOP!");
    }

    void simulateTravel(int floors) {
        pause(travelMillisPerFloor * floors);
    }

    private void pause(long millis) {
        if (millis == 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elevator simulation was interrupted", exception);
        }
    }
}
