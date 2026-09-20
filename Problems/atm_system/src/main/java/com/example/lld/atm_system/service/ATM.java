package com.example.lld.atm_system.service;

import com.example.lld.atm_system.model.Account;
import com.example.lld.atm_system.model.Card;
import com.example.lld.atm_system.state.ATMState;
import com.example.lld.atm_system.state.AuthenticatedState;
import com.example.lld.atm_system.state.CardInsertedState;
import com.example.lld.atm_system.state.IdleState;

public class ATM {
    private final BankingService bankingService;
    private final CashDispenser cashDispenser;
    private ATMState currentState;
    private Card currentCard;
    private int pinAttempts;

    public ATM(BankingService bankingService) {
        this.bankingService = bankingService;
        this.cashDispenser = new CashDispenser();
        this.currentState = new IdleState();
        this.pinAttempts = 0;
    }

    // State management
    public void setState(ATMState state) {
        this.currentState = state;
    }

    // Card operations
    public void insertCard(Card card) {
        currentState.insertCard(this, card);
    }

    public void ejectCard() {
        currentState.ejectCard(this);
    }

    // PIN operations
    public void enterPin(String pin) {
        currentState.enterPin(this, pin);
    }

    // Transaction operations
    public void selectOperation(String operation) {
        currentState.selectOperation(this, operation);
    }

    public void withdrawCash(double amount) {
        currentState.withdrawCash(this, amount);
    }

    public void checkBalance() {
        currentState.checkBalance(this);
    }

    public void changePin(String newPin) {
        currentState.changePin(this, newPin);
    }

    // Getters and setters
    public BankingService getBankingService() {
        return bankingService;
    }

    public CashDispenser getCashDispenser() {
        return cashDispenser;
    }

    public Card getCurrentCard() {
        return currentCard;
    }

    public void setCurrentCard(Card currentCard) {
        this.currentCard = currentCard;
    }

    public int getPinAttempts() {
        return pinAttempts;
    }

    public void incrementPinAttempts() {
        pinAttempts++;
    }

    public void resetPinAttempts() {
        pinAttempts = 0;
    }

    public void displayMenu() {
        System.out.println("\n=== ATM Menu ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Withdraw Cash");
        System.out.println("3. Change PIN");
        System.out.println("4. Exit");
        System.out.print("Select option: ");
    }
}
