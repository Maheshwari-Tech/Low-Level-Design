package com.example.lld.atm_system.state;

import com.example.lld.atm_system.service.ATM;
import com.example.lld.atm_system.model.Card;

public interface ATMState {
    void insertCard(ATM atm, Card card);
    void enterPin(ATM atm, String pin);
    void selectOperation(ATM atm, String operation);
    void withdrawCash(ATM atm, double amount);
    void checkBalance(ATM atm);
    void changePin(ATM atm, String newPin);
    void ejectCard(ATM atm);
}
