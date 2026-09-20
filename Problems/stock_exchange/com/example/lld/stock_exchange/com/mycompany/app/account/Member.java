package com.mycompany.app.account;

import java.util.*;

public class Member extends Account {
    private double availableFundsForTrading;
    private Date dateOfMembership;
    private HashMap<String, StockPosition> stockPositions;
    private HashMap<Integer, Order> activeOrders;

    public ErrorCode placeSellLimitOrder(String stockId, float quantity, int limitPrice, TimeEnforcementType enforcementType) {
        // Placeholder logic
        return ErrorCode.SUCCESS;
    }

    public ErrorCode placeBuyLimitOrder(String stockId, float quantity, int limitPrice, TimeEnforcementType enforcementType) {
        // Placeholder logic
        return ErrorCode.SUCCESS;
    }

    public void callbackStockExchange(int orderId, List<OrderPart> orderParts, OrderStatus status) {
        // definition
    }

    public boolean resetPassword() {
        // definition
        return true;
    }

    public void selectStock(String stockSymbol) {
        // Logic to select a stock
    }

    public Order selectOrderType(String orderType) {
        // Logic to return a specific order type (e.g., MarketOrder, LimitOrder)
        return null;
    }

    public double selectStockQty() {
        // Logic to select quantity
        return 0.0;
    }

    public double selectTimePriceLimit() {
        // Logic to set price/time limit
        return 0.0;
    }

    public TimeEnforcementType selectTimeEnforcement() {
        // Logic to select enforcement type
        return TimeEnforcementType.GOOD_TILL_CANCELED;
    }

    // Getters and Setters
    public double getAvailableFundsForTrading() {
        return availableFundsForTrading;
    }

    public void setAvailableFundsForTrading(double availableFundsForTrading) {
        this.availableFundsForTrading = availableFundsForTrading;
    }

    public Date getDateOfMembership() {
        return dateOfMembership;
    }

    public void setDateOfMembership(Date dateOfMembership) {
        this.dateOfMembership = dateOfMembership;
    }

    public HashMap<String, StockPosition> getStockPositions() {
        return stockPositions;
    }

    public void setStockPositions(HashMap<String, StockPosition> stockPositions) {
        this.stockPositions = stockPositions;
    }

    public HashMap<Integer, Order> getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(HashMap<Integer, Order> activeOrders) {
        this.activeOrders = activeOrders;
    }
}
