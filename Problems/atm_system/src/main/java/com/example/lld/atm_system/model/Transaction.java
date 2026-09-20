package com.example.lld.atm_system.model;

import java.time.LocalDateTime;

public record Transaction(String type, double amount, double balance, LocalDateTime timestamp) {
    public Transaction(String type, double amount, double balance) {
        this(type, amount, balance, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f (Balance: $%.2f) at %s",
            type, amount, balance, timestamp);
    }
}
