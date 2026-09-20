package com.example.lld.atm_system.state;
import com.example.lld.atm_system.service.ATM;
import com.example.lld.atm_system.model.Card;
import com.example.lld.atm_system.model.Account;

public class CardInsertedState implements ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("Card already inserted.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        if (atm.getBankingService().authenticate(atm.getCurrentCard().accountNumber(), pin)) {
            atm.setState(new AuthenticatedState());
            System.out.println("PIN accepted. Please select operation.");
        } else {
            System.out.println("Invalid PIN. Please try again.");
            atm.incrementPinAttempts();
            if (atm.getPinAttempts() >= 3) {
                System.out.println("Too many failed attempts. Card retained.");
                atm.ejectCard();
            }
        }
    }

    @Override
    public void selectOperation(ATM atm, String operation) {
        System.out.println("Please enter PIN first.");
    }

    @Override
    public void withdrawCash(ATM atm, double amount) {
        System.out.println("Please enter PIN first.");
    }

    @Override
    public void checkBalance(ATM atm) {
        System.out.println("Please enter PIN first.");
    }

    @Override
    public void changePin(ATM atm, String newPin) {
        System.out.println("Please enter PIN first.");
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.setCurrentCard(null);
        atm.setState(new IdleState());
        atm.resetPinAttempts();
        System.out.println("Card ejected.");
    }
}
