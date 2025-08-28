package com.mycompany.app.payment;

public class CreditCard extends Payment{
    public CreditCard(double amt) {
        super(amt);
    }

    @Override
    public boolean initiateTransaction() {
        status = PaymentStatus.SUCCESS;
        System.out.println("Card payment of : rs" + amount + " done.");
        return true;
    }
}
