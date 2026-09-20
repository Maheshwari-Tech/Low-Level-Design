package com.example.lld.elevator_system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ElevatorSystem {
    private final List<Elevator> elevators = new ArrayList<>();
    private final Map<Integer, Floor> floors = new LinkedHashMap<>();
    private final ElevatorController controller;
    private final int numFloors;

    public ElevatorSystem(int numElevators, int numFloors) {
        this(numElevators, numFloors, new ElevatorController());
    }

    public ElevatorSystem(int numElevators, int numFloors, ElevatorController controller) {
        if (numElevators <= 0 || numFloors <= 1) {
            throw new IllegalArgumentException("At least one elevator and two floors are required");
        }
        this.numFloors = numFloors;
        this.controller = Objects.requireNonNull(controller, "controller");

        for (int id = 1; id <= numElevators; id++) {
            elevators.add(new Elevator(id, controller));
        }
        for (int floor = 0; floor < numFloors; floor++) {
            floors.put(floor, new Floor(floor));
        }
        System.out.printf("Elevator System Started - %d elevators, %d floors%n", numElevators, numFloors);
    }

    public void requestElevator(int floor, Direction direction) {
        validateFloor(floor);
        if (direction == null || direction == Direction.IDLE) {
            throw new IllegalArgumentException("Direction must be UP or DOWN");
        }
        if ((floor == 0 && direction == Direction.DOWN)
                || (floor == numFloors - 1 && direction == Direction.UP)) {
            throw new IllegalArgumentException("Direction points outside the building");
        }

        Floor targetFloor = floors.get(floor);
        targetFloor.addRequest(direction);
        Elevator bestElevator = findBestElevator(floor, direction);
        if (bestElevator == null) {
            System.out.println("No elevator is currently available for floor " + floor);
            return;
        }

        controller.moveElevator(bestElevator, floor);
        targetFloor.markServed(direction);
    }

    public void selectFloor(int elevatorId, int floor) {
        validateFloor(floor);
        if (elevatorId < 1 || elevatorId > elevators.size()) {
            throw new IllegalArgumentException("Invalid elevator id: " + elevatorId);
        }
        elevators.get(elevatorId - 1).addInternalRequest(floor);
    }

    public void displayStatus() {
        System.out.println("\n=== Elevator Status ===");
        elevators.stream()
                .sorted(Comparator.comparingInt(Elevator::getId))
                .forEach(elevator -> System.out.printf(
                        "Elevator %d: Floor %d, State: %s, Direction: %s%n",
                        elevator.getId(),
                        elevator.getCurrentFloor(),
                        elevator.getState(),
                        elevator.getDirection()));
    }

    public List<Elevator> getElevators() {
        return List.copyOf(elevators);
    }

    public Map<Integer, Floor> getFloors() {
        return Map.copyOf(floors);
    }

    private Elevator findBestElevator(int floor, Direction direction) {
        return elevators.stream()
                .filter(elevator -> elevator.isIdle()
                        || (direction == Direction.UP
                        && elevator.isGoingUp()
                        && elevator.getCurrentFloor() <= floor)
                        || (direction == Direction.DOWN
                        && elevator.isGoingDown()
                        && elevator.getCurrentFloor() >= floor))
                .min(Comparator.comparingInt(elevator -> Math.abs(elevator.getCurrentFloor() - floor)))
                .orElse(null);
    }

    private void validateFloor(int floor) {
        if (floor < 0 || floor >= numFloors) {
            throw new IllegalArgumentException("Floor must be between 0 and " + (numFloors - 1));
        }
    }
}
