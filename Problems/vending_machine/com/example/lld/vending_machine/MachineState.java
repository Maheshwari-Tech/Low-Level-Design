package com.example.lld.vending_machine;

public interface MachineState {
    void selectProduct(VendingMachine machine, String productCode);

    void insertMoney(VendingMachine machine, double amount);

    void dispenseProduct(VendingMachine machine);

    void cancelTransaction(VendingMachine machine);
}
