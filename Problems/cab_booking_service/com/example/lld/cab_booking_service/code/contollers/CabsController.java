package code.contollers;

import code.db.CabsManager;
import code.db.TripsManager;
import code.model.Location;

public class CabsController {
    private CabsManager cabsManager;
    private TripsManager tripsManager;

    public void register(String licenceNo, String driverName){
        cabsManager.registerCab(licenceNo, driverName);
    }

    public void updateLocation(Location location, String cabNo){
        cabsManager.updateLocation(location, cabNo);
    }

    public void turnOnline(String licenceNo){
        cabsManager.updateStatus(licenceNo,true);
    }

    public void turnOffline(String licenceNo){
        cabsManager.updateStatus(licenceNo,false);
    }

}
