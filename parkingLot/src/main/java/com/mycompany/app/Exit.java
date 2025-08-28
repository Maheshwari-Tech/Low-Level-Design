package com.mycompany.app;

import com.mycompany.app.vehicles.Vehicle;

import java.util.Scanner;

public class Exit extends Gate{

    Scanner sc = new Scanner(System.in);

    @Override
    void process(Vehicle vehicle) {
        System.out.println("enter option to choose 1. credit card 2. Cash");
        sc.nextInt();

    }
}
