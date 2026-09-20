package com.mycompany.app;

import java.util.*;

public class StockInventory implements Search {
    private String inventoryName;
    private Date lastUpdate;

    public Stock searchSymbol(String symbol) {
        // definition
        return null;
    }

    public boolean deductStock(String symbol, double quantity) {
        // Logic to deduct quantity of a stock if available
        return true;
    }

    public boolean sendOrderDetails(Order order) {
        // Logic to forward order to stock exchange or another component
        return true;
    }

    // Getters and Setters
    public String getInventoryName() {
        return inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
