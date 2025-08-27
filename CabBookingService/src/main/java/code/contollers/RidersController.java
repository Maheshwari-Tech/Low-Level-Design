package code.contollers;

import code.db.RidersManager;
import code.db.TripsManager;

public class RidersController {
    private RidersManager ridersManager;
    private TripsManager tripsManager;

    public void register(String name){
        ridersManager.registerRider(name);
    }

}
