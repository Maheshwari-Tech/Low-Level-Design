package com.example.lld.atm_system.service;

import com.example.lld.atm_system.model.Account;
import java.util.HashMap;
import java.util.Map;

public class SimpleBankingService implements BankingService {
    private final Map<String, Account> accounts;

    public SimpleBankingService() {
        this.accounts = new HashMap<>();
        // Initialize with sample accounts
        Account account1 = new Account("123456789", 2500.00, "1234");
        Account account2 = new Account("987654321", 1500.00, "5678");

        accounts.put(account1.getAccountNumber(), account1);
        accounts.put(account2.getAccountNumber(), account2);
    }

    @Override
    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    @Override
    public boolean authenticate(String accountNumber, String pin) {
        return accounts.values().stream()
            .anyMatch(account -> account.getAccountNumber().equals(accountNumber) &&
                                account.validatePin(pin));
    }

    @Override
    public boolean withdraw(String accountNumber, double amount) {
        Account account = accounts.get(accountNumber);
        return account != null && account.withdraw(amount);
    }

    @Override
    public double getBalance(String accountNumber) {
        Account account = accounts.get(accountNumber);
        return account != null ? account.getBalance() : 0.0;
    }

    @Override
    public boolean changePin(String accountNumber, String oldPin, String newPin) {
        Account account = accounts.get(accountNumber);
        if (account != null && account.validatePin(oldPin)) {
            account.changePin(newPin);
            return true;
        }
        return false;
    }
}
