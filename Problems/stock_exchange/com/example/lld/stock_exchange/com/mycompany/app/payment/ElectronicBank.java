package com.mycompany.app.payment;

import java.util.*;

public class ElectronicBank extends TransferMoney {
    private String bankName;

    @Override
    public boolean initiateTransaction() {
        // Logic to transfer using electronic bank
        return true;
    }

    // Getter and Setter
    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}