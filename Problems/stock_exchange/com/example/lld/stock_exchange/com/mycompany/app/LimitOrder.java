package com.mycompany.app;

import java.util.*;

public class LimitOrder extends Order {
    private double priceLimit;

    public double getPriceLimit() {
        return priceLimit;
    }

    public void setPriceLimit(double priceLimit) {
        this.priceLimit = priceLimit;
    }
}
