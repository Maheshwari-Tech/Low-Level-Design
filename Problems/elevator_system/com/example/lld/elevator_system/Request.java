package com.example.lld.elevator_system;

import java.util.Objects;

public record Request(int floor, Direction direction) {
    public Request {
        if (floor < 0) {
            throw new IllegalArgumentException("Floor cannot be negative");
        }
        Objects.requireNonNull(direction, "direction");
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("A floor request must be UP or DOWN");
        }
    }
}
