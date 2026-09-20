package com.example.lld.atm_system.service;

import java.util.HashMap;
import java.util.Map;

public class CashDispenser {
    private final Map<Integer, Integer> cashInventory;

    public CashDispenser() {
        this.cashInventory = new HashMap<>();
        // Initialize with some cash
        cashInventory.put(100, 10); // 10 x $100 bills
        cashInventory.put(50, 20);  // 20 x $50 bills
        cashInventory.put(20, 50);  // 50 x $20 bills
        cashInventory.put(10, 100); // 100 x $10 bills
        cashInventory.put(5, 200);  // 200 x $5 bills
        cashInventory.put(1, 500);  // 500 x $1 bills
    }

    public boolean dispenseCash(int amount) {
        Map<Integer, Integer> toDispense = calculateDispensation(amount);
        if (toDispense.isEmpty()) {
            return false;
        }

        // Update inventory
        for (Map.Entry<Integer, Integer> entry : toDispense.entrySet()) {
            int denomination = entry.getKey();
            int count = entry.getValue();
            cashInventory.put(denomination, cashInventory.get(denomination) - count);
        }

        System.out.println("Dispensing:");
        for (Map.Entry<Integer, Integer> entry : toDispense.entrySet()) {
            System.out.printf("  %d x $%d%n", entry.getValue(), entry.getKey());
        }
        return true;
    }

    private Map<Integer, Integer> calculateDispensation(int amount) {
        Map<Integer, Integer> result = new HashMap<>();
        int[] denominations = {100, 50, 20, 10, 5, 1};

        for (int denom : denominations) {
            if (amount >= denom) {
                int available = cashInventory.get(denom);
                int needed = amount / denom;
                int toTake = Math.min(needed, available);
                if (toTake > 0) {
                    result.put(denom, toTake);
                    amount -= toTake * denom;
                }
            }
        }

        return amount == 0 ? result : new HashMap<>();
    }

    public int getTotalCash() {
        return cashInventory.entrySet().stream()
            .mapToInt(entry -> entry.getKey() * entry.getValue())
            .sum();
    }

    public boolean canDispense(int amount) {
        return cashInventory.entrySet().stream()
            .mapToInt(entry -> entry.getKey() * entry.getValue())
            .sum() >= amount;
    }

    public Map<Integer, Integer> getAvailableDenominations() {
        return cashInventory.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    public void displayInventory() {
        System.out.println("ATM Cash Inventory:");
        for (Map.Entry<Integer, Integer> entry : cashInventory.entrySet()) {
            System.out.printf("  $%d: %d bills%n", entry.getKey(), entry.getValue());
        }
        System.out.printf("Total cash: $%d%n", getTotalCash());
    }
}
