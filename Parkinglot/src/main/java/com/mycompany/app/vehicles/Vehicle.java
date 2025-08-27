package com.mycompany.app.vehicles;

import com.mycompany.app.ParkingTicket;

public abstract class Vehicle {
    private final String licenseNo;
    public Vehicle(String lic) {
        this.licenseNo = lic;
    }
    public String getLicenseNo() {
        return licenseNo;
    }
}
