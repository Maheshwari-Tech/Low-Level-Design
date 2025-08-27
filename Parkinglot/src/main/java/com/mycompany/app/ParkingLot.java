package com.mycompany.app;

import com.mycompany.app.spots.Spot;
import com.mycompany.app.spots.SpotCreationFactory;
import com.mycompany.app.spots.SpotType;

import java.util.HashMap;
import java.util.Map;

public class ParkingLot {

    Map<Integer, Gate> entryGates = new HashMap<>();
    Map<Integer, Gate> exitGates = new HashMap<>();
    Map<Integer, Spot> spots;

    private static ParkingLot instance;
    SpotCreationFactory spotCreationFactory = new SpotCreationFactory();

    private ParkingLot(){
    }

    public static ParkingLot getInstance(){
        if (instance == null) instance = new ParkingLot();
        return instance;
    }

    public void addEntryGates(int n){
       for(int i=0; i<n; ++i){
           Gate gate = new Entrance();
           entryGates.put(gate.getId(), gate);
       }
    }
    public void addExitGate(int n){
        for(int i=0; i<n; ++i){
            Gate gate = new Exit();
            exitGates.put(gate.getId(), gate);
        }
    }

    public void addSpot(SpotType spotType, int count){
        for(int i=0; i<count; ++i){
            Spot spot = spotCreationFactory.createSpot(spotType);
            spots.put(spot.getId(), spot);
        }
    }

    public void rankSpotByGate(){

    }

    public void rankSpotByGate(int spotId, int gateId, int rank) {

    }
}
