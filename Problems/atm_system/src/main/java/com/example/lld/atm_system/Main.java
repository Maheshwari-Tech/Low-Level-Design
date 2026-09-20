package com.example.lld.atm_system;

import com.example.lld.atm_system.model.Card;
import com.example.lld.atm_system.service.ATM;
import com.example.lld.atm_system.service.BankingService;
import com.example.lld.atm_system.service.SimpleBankingService;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        BankingService bankingService = new SimpleBankingService();
        ATM atm = new ATM(bankingService);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== ATM System Demo ===");
        System.out.println("Available test cards:");
        System.out.println("Card: 123456789, PIN: 1234 (Balance: $2500)");
        System.out.println("Card: 987654321, PIN: 5678 (Balance: $1500)");

        // Simulate card insertion
        System.out.print("\nEnter card number: ");
        String cardNumber = scanner.nextLine();
        Card card = new Card(cardNumber, cardNumber); // Simplified: card number = account number
        atm.insertCard(card);

        // PIN entry
        System.out.print("Enter PIN: ");
        String pin = scanner.nextLine();
        atm.enterPin(pin);

        // Main menu loop
        boolean running = true;
        while (running) {
            atm.displayMenu();
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    atm.checkBalance();
                    break;
                case 2:
                    System.out.print("Enter withdrawal amount: ");
                    double amount = scanner.nextDouble();
                    atm.withdrawCash(amount);
                    break;
                case 3:
                    System.out.print("Enter new PIN: ");
                    String newPin = scanner.next();
                    atm.changePin(newPin);
                    break;
                case 4:
                    atm.ejectCard();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }

        scanner.close();
        System.out.println("Session ended.");
    }
}
