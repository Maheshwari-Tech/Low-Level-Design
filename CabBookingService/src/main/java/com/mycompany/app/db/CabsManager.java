package com.mycompany.app.db;

import com.mycompany.app.exceptions.CabsAlreadyExistsException;
import com.mycompany.app.exceptions.NoCabsAvailableException;
import com.mycompany.app.model.Cab;
import com.mycompany.app.model.Location;

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
