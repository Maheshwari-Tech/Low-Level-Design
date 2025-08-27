package com.mycompany.app;

import com.mycompany.app.payment.Payment;
import com.mycompany.app.payment.PaymentOption;
import com.mycompany.app.payment.PaymentService;
import com.mycompany.app.vehicles.Vehicle;

import java.util.Map;

public class ParkingTicketService {

    Map<Integer, Integer> vechileToParkIdMap;
    Map<Integer, Park> parkMap;
    PaymentService paymentService = new PaymentService();
    PriceCalculatorService priceCalculatorService = new PriceCalculatorService();


     ParkingTicket generateTicket(Vehicle vehicle){
         ParkingTicket t = new ParkingTicket(vehicle);
         return t;
     }

     void processExit(ParkingTicket ticket){
         ticket.updateExit();

         PaymentOption option = PaymentOption.CASH;

         double price = priceCalculatorService.calculate(ticket, option);
         Payment payment = paymentService.createPayment(price, option);

         ticket.updatePayment(payment);

     }
}
