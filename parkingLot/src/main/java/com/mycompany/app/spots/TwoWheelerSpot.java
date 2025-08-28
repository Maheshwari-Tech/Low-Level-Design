package com.mycompany.app.spots;

import java.util.concurrent.atomic.AtomicInteger;

public class TwoWheelerSpot extends Spot{
    int id;
    SpotType spotType;
    AtomicInteger gateId = new AtomicInteger(0);

    TwoWheelerSpot(){
        this.id = gateId.getAndIncrement();
        this.spotType = SpotType.TWO_WHEELER;
    }

    public int getId() {
        return id;
    }
}
