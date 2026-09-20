package com.example.lld.atm_system.state;
import com.example.lld.atm_system.service.ATM;
import com.example.lld.atm_system.model.Card;
import com.example.lld.atm_system.model.Account;

public class IdleState implements ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        atm.setCurrentCard(card);
        atm.setState(new CardInsertedState());
        System.out.println("Card inserted. Please enter PIN.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Please insert card first.");
    }

    @Override
    public void selectOperation(ATM atm, String operation) {
        System.out.println("Please insert card and authenticate first.");
    }

    @Override
    public void withdrawCash(ATM atm, double amount) {
        System.out.println("Please insert card and authenticate first.");
    }

    @Override
    public void checkBalance(ATM atm) {
        System.out.println("Please insert card and authenticate first.");
    }

    @Override
    public void changePin(ATM atm, String newPin) {
        System.out.println("Please insert card and authenticate first.");
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("No card to eject.");
    }
}
