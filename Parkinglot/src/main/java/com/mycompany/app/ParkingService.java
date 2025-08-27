package com.mycompany.app;

import com.mycompany.app.spots.Spot;
import com.mycompany.app.vehicles.Vehicle;

import java.util.Map;

public class ParkingService {

    Map<Integer, Integer> vechileToParkIdMap;
    Map<Integer, Park> parkMap;

    void park(Vehicle vehicle, Spot spot){
        Park p = new Park(spot, vehicle);


    }


    void unpark(Vehicle vehicle){

    }

    void unpark(Spot spot){

    }
}
