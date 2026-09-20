package code.contollers;

import code.db.RidersManager;
import code.db.TripsManager;
import code.model.Location;

public class RidersController {
    private RidersManager ridersManager;
    public void register(String name){
        ridersManager.registerRider(name);
    }
}
