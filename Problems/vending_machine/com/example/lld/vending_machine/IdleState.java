package com.example.lld.vending_machine;

public final class IdleState implements MachineState {
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        if (!machine.getInventory().isAvailable(productCode)) {
            System.out.println("Product not available or out of stock.");
            return;
        }
        machine.setSelectedProduct(productCode);
        machine.setState(new SelectingState());
        System.out.println("Product selected: " + productCode);
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        System.out.println("Please select a product first.");
    }

    @Override
    public void dispenseProduct(VendingMachine machine) {
        System.out.println("No product selected.");
    }

    @Override
    public void cancelTransaction(VendingMachine machine) {
        System.out.println("No transaction to cancel.");
    }
}
