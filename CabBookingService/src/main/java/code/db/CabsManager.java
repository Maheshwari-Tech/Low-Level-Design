package code.db;

import code.exceptions.CabsAlreadyExistsException;
import code.exceptions.NoCabsAvailableException;
import code.model.Cab;
import code.model.Location;

import java.util.HashMap;
import java.util.Map;

public class CabsManager {
    private final Map<String, Cab> cabs;

    CabsManager(){
        cabs = new HashMap<>();
    }

    public void registerCab(String licenceNo, String driverName){
        if(cabs.containsKey(licenceNo)){
            throw new CabsAlreadyExistsException("can't add cab " + licenceNo);
        }
        Cab cab = new Cab(licenceNo, driverName);
        cabs.put(licenceNo, cab);
    }

    public void updateLocation(Location location, String licenceNo){
        if(!cabs.containsKey(licenceNo)){
            throw new NoCabsAvailableException("can't find cab " + licenceNo);
        }
        cabs.get(licenceNo).setCurrentLocation(location);
    }

    public void updateStatus(String licenceNo, boolean status){
        if(!cabs.containsKey(licenceNo)){
            throw new NoCabsAvailableException("can't find cab " + licenceNo);
        }
        cabs.get(licenceNo).setActive(status);
    }
}
