package com.example.lld.atm_system.state;
import com.example.lld.atm_system.service.ATM;
import com.example.lld.atm_system.model.Card;
import com.example.lld.atm_system.model.Account;

import com.example.lld.atm_system.model.Account;

public class AuthenticatedState implements ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("Already authenticated.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Already authenticated.");
    }

    @Override
    public void selectOperation(ATM atm, String operation) {
        System.out.println("Selected: " + operation);
        // Operation will be handled by specific methods
    }

    @Override
    public void withdrawCash(ATM atm, double amount) {
        String accountNumber = atm.getCurrentCard().accountNumber();
        double balance = atm.getBankingService().getBalance(accountNumber);

        if (amount > balance) {
            System.out.println("Insufficient funds.");
            return;
        }

        if (amount > atm.getCashDispenser().getTotalCash()) {
            System.out.println("ATM has insufficient cash.");
            return;
        }

        if (atm.getBankingService().withdraw(accountNumber, amount)) {
            if (atm.getCashDispenser().dispenseCash((int) amount)) {
                System.out.println("Withdrawal successful.");
            }
        } else {
            System.out.println("Withdrawal failed.");
        }
    }

    @Override
    public void checkBalance(ATM atm) {
        String accountNumber = atm.getCurrentCard().accountNumber();
        Account account = atm.getBankingService().getAccount(accountNumber);
        double balance = account.getBalance();

        System.out.printf("Current balance: $%.2f%n", balance);

        // Show recent transactions using streams
        System.out.println("Recent transactions:");
        account.getTransactions().stream()
            .sorted((t1, t2) -> t2.timestamp().compareTo(t1.timestamp()))
            .limit(5)
            .forEach(System.out::println);
    }

    @Override
    public void changePin(ATM atm, String newPin) {
        String accountNumber = atm.getCurrentCard().accountNumber();
        // In real implementation, would need current PIN verification
        System.out.println("PIN change functionality would be implemented here.");
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.setCurrentCard(null);
        atm.setState(new IdleState());
        atm.resetPinAttempts();
        System.out.println("Thank you for using our ATM. Card ejected.");
    }
}
