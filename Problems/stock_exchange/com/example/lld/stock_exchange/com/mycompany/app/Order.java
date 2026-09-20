package com.mycompany.app;

import java.util.*;

public abstract class Order {
    private String orderNumber;
    public boolean isBuyOrder;
    private OrderStatus status;
    private TimeEnforcementType timeEnforcement;
    private Date creationTime;
    private HashMap<Integer, OrderPart> parts;

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public boolean saveInDatabase() {
        // Logic to persist the order
        return true;
    }

    public void addOrderParts(OrderPart part) {
        if (this.parts == null) {
            this.parts = new HashMap<>();
        }
        this.parts.put(parts.size(), part);
    }

    // Getters and Setters
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public boolean getIsBuyOrder() {
        return isBuyOrder;
    }

    public void setIsBuyOrder(boolean isBuyOrder) {
        this.isBuyOrder = isBuyOrder;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public TimeEnforcementType getTimeEnforcement() {
        return timeEnforcement;
    }

    public void setTimeEnforcement(TimeEnforcementType timeEnforcement) {
        this.timeEnforcement = timeEnforcement;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public HashMap<Integer, OrderPart> getParts() {
        return parts;
    }

    public void setParts(HashMap<Integer, OrderPart> parts) {
        this.parts = parts;
    }
}
