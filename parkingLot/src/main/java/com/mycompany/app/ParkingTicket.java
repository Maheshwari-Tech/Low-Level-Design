package com.mycompany.app;

import com.mycompany.app.payment.Payment;
import com.mycompany.app.vehicles.Vehicle;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class ParkingTicket {
    int id;
    LocalDateTime entryTime;
    LocalDateTime exitTime;
    Vehicle vehicle;
    double price;
    Payment payment;

    static volatile AtomicInteger atomicInteger = new AtomicInteger(1);

    ParkingTicket(Vehicle vehicle){
        this.vehicle = vehicle;
        this.id = atomicInteger.getAndIncrement();
        this.entryTime = LocalDateTime.now();
    }

    void updateExit(){
        this.exitTime = LocalDateTime.now();
    }

    void updatePrice(double price){
        this.price =  price;
    }

    void updatePayment(Payment payment){
       this.payment = payment;
    }
}
