package com.example.lld.vending_machine;

import java.util.Objects;

public final class VendingMachine {
    private final Inventory inventory;
    private final PaymentProcessor paymentProcessor;

    private MachineState currentState = new IdleState();
    private String selectedProductCode;

    public VendingMachine() {
        this(new Inventory(), true);
    }

    public VendingMachine(Inventory inventory) {
        this(inventory, false);
    }

    private VendingMachine(Inventory inventory, boolean seedDemoProducts) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.paymentProcessor = new PaymentProcessor();
        if (seedDemoProducts) {
            initializeDemoInventory();
        }
    }

    public void selectProduct(String productCode) {
        currentState.selectProduct(this, productCode);
    }

    public void insertMoney(double amount) {
        currentState.insertMoney(this, amount);
    }

    public void dispenseProduct() {
        currentState.dispenseProduct(this);
    }

    public void cancelTransaction() {
        currentState.cancelTransaction(this);
    }

    public void displayProducts() {
        inventory.displayProducts();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public PaymentProcessor getPaymentProcessor() {
        return paymentProcessor;
    }

    public MachineState getCurrentState() {
        return currentState;
    }

    public String getSelectedProduct() {
        return selectedProductCode;
    }

    void setState(MachineState state) {
        currentState = Objects.requireNonNull(state, "state");
    }

    void setSelectedProduct(String selectedProductCode) {
        this.selectedProductCode = selectedProductCode;
    }

    Product selectedProduct() {
        if (selectedProductCode == null) {
            throw new IllegalStateException("No product is selected");
        }
        Product product = inventory.getProduct(selectedProductCode);
        if (product == null) {
            throw new IllegalStateException("Selected product no longer exists");
        }
        return product;
    }

    void resetSelection() {
        selectedProductCode = null;
        currentState = new IdleState();
    }

    private void initializeDemoInventory() {
        inventory.addProduct(new Product("A1", "Chips", 1.50, 5));
        inventory.addProduct(new Product("A2", "Soda", 2.00, 3));
        inventory.addProduct(new Product("B1", "Candy", 1.00, 10));
        inventory.addProduct(new Product("B2", "Water", 1.25, 7));
    }
}
