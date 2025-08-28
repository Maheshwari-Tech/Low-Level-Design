package com.mycompany.app.payment;

public class Cash extends Payment{
    public Cash(double amt) {
        super(amt);
    }

    @Override
    public boolean initiateTransaction() {
        status = PaymentStatus.SUCCESS;
        System.out.println("Cash payment of : Rs" + amount + " done.");
        return true;
    }
}
