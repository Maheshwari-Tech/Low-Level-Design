package com.example.lld.vending_machine;

import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Vending Machine Demo ===");
        while (true) {
            System.out.println("\n1. Display products\n2. Select product\n3. Insert money"
                    + "\n4. Dispense product\n5. Cancel transaction\n6. Exit");
            System.out.print("Enter command: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Enter a numeric command.");
                scanner.nextLine();
                continue;
            }

            int command = scanner.nextInt();
            scanner.nextLine();
            switch (command) {
                case 1 -> machine.displayProducts();
                case 2 -> {
                    System.out.print("Enter product code: ");
                    machine.selectProduct(scanner.nextLine().trim());
                }
                case 3 -> {
                    System.out.print("Enter amount: ");
                    if (scanner.hasNextDouble()) {
                        machine.insertMoney(scanner.nextDouble());
                        scanner.nextLine();
                    } else {
                        System.out.println("Enter a numeric amount.");
                        scanner.nextLine();
                    }
                }
                case 4 -> machine.dispenseProduct();
                case 5 -> machine.cancelTransaction();
                case 6 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid command.");
            }
        }
    }
}
