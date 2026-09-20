package com.mycompany.app.payment;

public class DepositMoney {
    private int transactionId;

    public boolean initiateTransaction() {
        // Logic to deposit funds
        return true;
    }

    // Getter and Setter
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
}
