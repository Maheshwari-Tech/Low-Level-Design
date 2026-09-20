package com.mycompany.app;

import java.util.*;

public class Watchlist {
    private String name;
    private List<Stock> stocks;

    public List<Stock> getStocks() {
        return stocks;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }
}
