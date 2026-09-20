package com.mycompany.app.payment;

import java.util.*;

public class Check extends TransferMoney {
    private String checkNumber;

    @Override
    public boolean initiateTransaction() {
        // Logic to transfer using check
        return true;
    }

    // Getter and Setter
    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }
}

