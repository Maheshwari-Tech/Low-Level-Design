package com.mycompany.app.models;

public record ReadResponse<Value>(Value value, double timeTaken) {
}
