package com.mycompany.app.order_processing_system;

import java.util.*;

public class OrderProcessingServiceImpl implements OrderProcessingService{
        // One queue per priority
        private final Map<Priority, LinkedList<Order>> queues = new EnumMap<>(Priority.class);
        // For fast lookup & cancellation
        private final Map<String, Order> orderMap = new HashMap<>();

        public OrderProcessingServiceImpl() {
            for (Priority p : Priority.values()) {
                queues.put(p, new LinkedList<>());
            }
        }

        // 1. Add order
        public void addOrder(Order order) {
            queues.get(order.getPriority()).addLast(order);
            orderMap.put(order.getId(), order);
        }





    // 2. Process next order
        public Order processNext() {
            for (Priority p : Priority.values()) {
                LinkedList<Order> q = queues.get(p);
                if (!q.isEmpty()) {
                    Order next = q.removeFirst();
                    orderMap.remove(next.getId());
                    return next;
                }
            }
            return null; // no orders
        }

        // 3. Cancel order by ID
        public boolean cancelOrder(String orderId) {
            Order o = orderMap.remove(orderId);
            if (o == null) return false;
            return queues.get(o.getPriority()).remove(o);
        }

        // 4. Get orders by priority
        public List<Order> getOrdersByPriority(Priority priority) {
            return new ArrayList<>(queues.get(priority));
        }

        @Override
        public int getEstimatedProcessingTime(String orderId) {
            Order target = orderMap.get(orderId);
            if (target == null) return -1; // not found / already processed

            int total = 0;
            // Sum all orders in higher or equal priority queues that are ahead of target
            for (Priority p : Priority.values()) {
                LinkedList<Order> q = queues.get(p);
                for (Order o : q) {
                    if (p.getRank() < target.getPriority().getRank()) {
                        total += o.getProcessingTime(); // higher priority always ahead
                    } else if (p == target.getPriority()) {
                        if (o == target) return total; // reached our order
                        total += o.getProcessingTime();
                    }
                }
            }
            return total;
        }
}

