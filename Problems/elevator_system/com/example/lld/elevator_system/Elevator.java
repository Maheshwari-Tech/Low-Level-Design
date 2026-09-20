package com.example.lld.elevator_system;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

public final class Elevator {
    private final int id;
    private final Queue<Integer> internalRequests = new PriorityQueue<>();
    private final ElevatorController controller;

    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private ElevatorState state = ElevatorState.IDLE;

    public Elevator(int id, ElevatorController controller) {
        if (id <= 0) {
            throw new IllegalArgumentException("Elevator id must be positive");
        }
        this.id = id;
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void addInternalRequest(int floor) {
        if (floor < 0) {
            throw new IllegalArgumentException("Floor cannot be negative");
        }
        if (state == ElevatorState.EMERGENCY_STOP) {
            throw new IllegalStateException("Elevator " + id + " is in emergency stop");
        }
        if (!internalRequests.contains(floor)) {
            internalRequests.add(floor);
        }
        System.out.printf("Elevator %d: Internal request to floor %d%n", id, floor);
        if (state == ElevatorState.IDLE) {
            processNextRequest();
        }
    }

    public void moveToFloor(int targetFloor) {
        if (state == ElevatorState.EMERGENCY_STOP) {
            throw new IllegalStateException("Elevator " + id + " is in emergency stop");
        }

        int distance = Math.abs(targetFloor - currentFloor);
        if (targetFloor > currentFloor) {
            direction = Direction.UP;
            state = ElevatorState.MOVING_UP;
        } else if (targetFloor < currentFloor) {
            direction = Direction.DOWN;
            state = ElevatorState.MOVING_DOWN;
        }

        if (distance > 0) {
            System.out.printf("Elevator %d moving %s to floor %d%n", id, direction, targetFloor);
            controller.simulateTravel(distance);
            currentFloor = targetFloor;
        }

        arriveAtFloor();
    }

    public void emergencyStop() {
        internalRequests.clear();
        direction = Direction.IDLE;
        controller.emergencyStop(this);
    }

    public void resumeService() {
        if (state != ElevatorState.EMERGENCY_STOP) {
            return;
        }
        state = ElevatorState.IDLE;
        direction = Direction.IDLE;
    }

    public int getId() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public ElevatorState getState() {
        return state;
    }

    public boolean isIdle() {
        return state == ElevatorState.IDLE;
    }

    public boolean isGoingUp() {
        return direction == Direction.UP;
    }

    public boolean isGoingDown() {
        return direction == Direction.DOWN;
    }

    void transitionTo(ElevatorState newState) {
        state = newState;
    }

    private void arriveAtFloor() {
        System.out.printf("Elevator %d arrived at floor %d%n", id, currentFloor);
        controller.openDoors(this);
        controller.closeDoors(this);
        state = ElevatorState.IDLE;
        direction = Direction.IDLE;
        processNextRequest();
    }

    private void processNextRequest() {
        if (state != ElevatorState.IDLE) {
            return;
        }
        Integer nextFloor = internalRequests.poll();
        if (nextFloor == null) {
            System.out.println("Elevator " + id + " is now idle");
            return;
        }
        moveToFloor(nextFloor);
    }
}
