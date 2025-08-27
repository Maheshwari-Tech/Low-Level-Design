package com.mycompany.app.contollers;

import com.mycompany.app.db.RidersManager;
import com.mycompany.app.db.TripsManager;

public class RidersController {
    private RidersManager ridersManager;
    private TripsManager tripsManager;

    public void register(String name){
        ridersManager.registerRider(name);
    }

}
