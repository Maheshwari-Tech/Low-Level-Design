package com.example.lld.vending_machine;

public final class PaymentState implements MachineState {
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        System.out.println("Payment is complete. Dispense or cancel the current product first.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.getPaymentProcessor().insertMoney(amount);
    }

    @Override
    public void dispenseProduct(VendingMachine machine) {
        Product selectedProduct = machine.selectedProduct();
        double change = machine.getPaymentProcessor().calculateChange(selectedProduct.price());

        if (!machine.getInventory().reduceStock(selectedProduct.code())) {
            double refund = machine.getPaymentProcessor().refundAndReset();
            machine.resetSelection();
            System.out.printf("Product became unavailable. Refunded: $%.2f%n", refund);
            return;
        }

        System.out.println("Dispensing " + selectedProduct.name() + "...");
        if (change > 0) {
            System.out.printf("Change: $%.2f%n", change);
        }
        machine.getPaymentProcessor().reset();
        machine.resetSelection();
    }

    @Override
    public void cancelTransaction(VendingMachine machine) {
        double refund = machine.getPaymentProcessor().refundAndReset();
        machine.resetSelection();
        System.out.printf("Transaction cancelled. Refunded: $%.2f%n", refund);
    }
}
