package com.mycompany.app.payment;

import java.util.*;

public class Wire extends TransferMoney {
    private int wire;

    @Override
    public boolean initiateTransaction() {
        // Logic to transfer using wire
        return true;
    }

    // Getter and Setter
    public int getWire() {
        return wire;
    }

    public void setWire(int wire) {
        this.wire = wire;
    }
}