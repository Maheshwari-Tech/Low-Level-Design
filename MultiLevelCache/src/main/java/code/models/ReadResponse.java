package code.models;

public record ReadResponse<Value>(Value value, double timeTaken) {
}
