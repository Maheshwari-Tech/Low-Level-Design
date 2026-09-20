package com.mycompany.app.order_processing_system;

import java.util.List;
import java.util.Map;

public interface OrderProcessingService {
    void addOrder(Order order);
    Order processNext();
    boolean cancelOrder(String orderId);
    List<Order> getOrdersByPriority(Priority priority);
    int getEstimatedProcessingTime(String orderId);
}
