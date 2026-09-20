package com.mycompany.app;

import java.util.*;

public class StockLot {
    private String lotNumber;
    private Order buyingOrder;

    public double getBuyingPrice() {
        // Assuming price is derived from buying order
        if (buyingOrder instanceof LimitOrder) {
            return ((LimitOrder) buyingOrder).getPriceLimit();
        }
        return 0.0;
    }

    // Getters and Setters
    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Order getBuyingOrder() {
        return buyingOrder;
    }

    public void setBuyingOrder(Order buyingOrder) {
        this.buyingOrder = buyingOrder;
    }
}

