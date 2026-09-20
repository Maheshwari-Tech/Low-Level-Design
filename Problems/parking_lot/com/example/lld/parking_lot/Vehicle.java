package com.example.lld.parking_lot;

public record Vehicle(String no, String color) {
    public Vehicle {
        if (no == null || no.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration number is required");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Vehicle color is required");
        }
    }
}
