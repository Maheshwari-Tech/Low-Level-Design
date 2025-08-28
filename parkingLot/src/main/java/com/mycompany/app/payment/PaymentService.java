package com.mycompany.app.payment;

import java.util.Map;

public class PaymentService {
    Map<Integer, Payment> payments;

    public boolean processPayment(int id){
       if(payments.containsKey(id)){
            return payments.get(id).initiateTransaction();
       }
       else{
           throw new RuntimeException(" payment id not found");
       }
    }

    public Payment createPayment(double amt, PaymentOption option){
        Payment payment = null;
        switch (option){
            case CASH -> {
                payment =  new Cash(amt);
            }
            case CREDIT_CARD -> {
                payment = new CreditCard(amt);
            }
        }
        if(payment == null){
            throw new RuntimeException(" Option not availble for payment");
        }
        payments.put(payment.getId(), payment);
        return payment;
    }
}
