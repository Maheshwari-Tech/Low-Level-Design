package com.example.lld.vending_machine;

public final class PaymentProcessor {
    private static final double EPSILON = 0.000_001;

    private double currentAmount;

    public void insertMoney(double amount) {
        if (amount <= 0 || !Double.isFinite(amount)) {
            throw new IllegalArgumentException("Inserted amount must be a finite positive value");
        }
        currentAmount += amount;
        System.out.printf("Inserted: $%.2f. Total: $%.2f%n", amount, currentAmount);
    }

    public boolean hasSufficientFunds(double requiredAmount) {
        return currentAmount + EPSILON >= requiredAmount;
    }

    public double calculateChange(double productPrice) {
        if (!hasSufficientFunds(productPrice)) {
            throw new IllegalStateException("Insufficient funds");
        }
        return Math.max(0, currentAmount - productPrice);
    }

    public double refundAndReset() {
        double refund = currentAmount;
        reset();
        return refund;
    }

    public void reset() {
        currentAmount = 0;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }
}
