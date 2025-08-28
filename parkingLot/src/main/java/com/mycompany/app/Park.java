package com.mycompany.app;

import com.mycompany.app.spots.Spot;
import com.mycompany.app.vehicles.Vehicle;

import java.time.LocalDateTime;

public class Park {
    Spot spot;
    Vehicle vehicle;

    LocalDateTime entryTime;
    LocalDateTime exitTime;

    Park(Spot spot, Vehicle vehicle){
        this.spot = spot;
        this.vehicle = vehicle;
        this.entryTime = LocalDateTime.now();
    }

    void unpark(){
        this.exitTime = LocalDateTime.now();
    }
}
