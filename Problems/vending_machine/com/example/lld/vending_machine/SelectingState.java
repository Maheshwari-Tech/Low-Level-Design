package com.example.lld.vending_machine;

public final class SelectingState implements MachineState {
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        System.out.println("Product already selected. Insert money or cancel the transaction.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.getPaymentProcessor().insertMoney(amount);
        Product selectedProduct = machine.selectedProduct();
        if (machine.getPaymentProcessor().hasSufficientFunds(selectedProduct.price())) {
            machine.setState(new PaymentState());
            System.out.println("Payment complete. Product is ready to dispense.");
        }
    }

    @Override
    public void dispenseProduct(VendingMachine machine) {
        System.out.println("Please complete payment first.");
    }

    @Override
    public void cancelTransaction(VendingMachine machine) {
        double refund = machine.getPaymentProcessor().refundAndReset();
        machine.resetSelection();
        System.out.printf("Transaction cancelled. Refunded: $%.2f%n", refund);
    }
}
