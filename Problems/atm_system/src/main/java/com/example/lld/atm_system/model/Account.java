package com.example.lld.atm_system.model;

import java.util.ArrayList;
import java.util.List;

public class Account {
    private final String accountNumber;
    private double balance;
    private String pin;
    private final List<Transaction> transactions;

    public Account(String accountNumber, double balance, String pin) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.pin = pin;
        this.transactions = new ArrayList<>();
    }

    public boolean validatePin(String inputPin) {
        return this.pin.equals(inputPin);
    }

    public void changePin(String newPin) {
        this.pin = newPin;
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            transactions.add(new Transaction("WITHDRAWAL", amount, balance));
            return true;
        }
        return false;
    }

    public void deposit(double amount) {
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, balance));
    }

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
}
