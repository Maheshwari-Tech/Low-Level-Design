package com.mycompany.app.payment;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Payment {
    int id;
    AtomicInteger counter = new AtomicInteger(0);

    protected double amount;
    protected PaymentStatus status;
    protected Date timestamp;

    public Payment(double amt) {
        this.id = counter.getAndIncrement();
        this.amount = amt;
        this.status = PaymentStatus.PENDING;
        this.timestamp = new Date();
    }

    public int getId() {
        return id;
    }

    public abstract boolean initiateTransaction();
}
