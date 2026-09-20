package com.mycompany.app;

import java.util.*;

public class StockExchange {
    private static StockExchange instance = null;

    private StockExchange() {}

    public static StockExchange getInstance() {
        if (instance == null) {
            instance = new StockExchange();
        }
        return instance;
    }

    public boolean placeOrder(Order order) {
        // Logic to place order in the exchange
        return true;
    }

    public boolean acknowledge(Order order) {
        // Logic to acknowledge order receipt
        return true;
    }
}
