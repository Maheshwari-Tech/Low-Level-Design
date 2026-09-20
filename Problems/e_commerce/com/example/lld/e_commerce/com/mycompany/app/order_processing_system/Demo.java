package com.mycompany.app.order_processing_system;

public class Demo {
    public static void main(String[] args) {
        OrderProcessingService ops = new OrderProcessingServiceImpl();

        ops.addOrder(new Order("O1", Priority.NORMAL, 5));
        ops.addOrder(new Order("O2", Priority.URGENT, 3));
        ops.addOrder(new Order("O3", Priority.HIGH, 4));
        ops.addOrder(new Order("O4", Priority.URGENT, 2));
        ops.addOrder(new Order("O5", Priority.NORMAL, 6));

        System.out.println("Estimated time for O5: " + ops.getEstimatedProcessingTime("O5") + "s");

        System.out.println("Processing: " + ops.processNext()); // O2 (URGENT)
        System.out.println("Processing: " + ops.processNext()); // O4 (URGENT)

        ops.cancelOrder("O3"); // Cancel HIGH priority order

        System.out.println("Orders in NORMAL: " + ops.getOrdersByPriority(Priority.NORMAL));

        System.out.println("Processing: " + ops.processNext()); // O1
        System.out.println("Processing: " + ops.processNext()); // O5
        System.out.println("Processing: " + ops.processNext()); // null (empty)
    }
}

