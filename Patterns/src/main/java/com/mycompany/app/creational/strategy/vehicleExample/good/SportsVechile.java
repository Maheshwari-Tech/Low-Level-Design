package com.mycompany.app.creational.strategy.vehicleExample.good;

public class SportsVechile extends Vehicle{
    public SportsVechile() {
        super(new SpecialDrive());
    }
}
