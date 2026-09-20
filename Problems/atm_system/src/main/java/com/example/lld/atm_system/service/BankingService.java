package com.example.lld.atm_system.service;

import com.example.lld.atm_system.model.Account;

public interface BankingService {
    Account getAccount(String accountNumber);
    boolean authenticate(String accountNumber, String pin);
    boolean withdraw(String accountNumber, double amount);
    double getBalance(String accountNumber);
    boolean changePin(String accountNumber, String oldPin, String newPin);
}
