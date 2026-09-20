package com.mycompany.app.order_processing_system;

public class Order {
    private final String id;
    private final Priority priority;
    private final int processingTime;

    public Order(String id, Priority priority, int processingTime) {
        this.id = id;
        this.priority = priority;
        this.processingTime = processingTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Order{" + id + ", " + priority + ", " + processingTime + "s}";
    }
}
