package code.db;

import code.exceptions.RidersAlreadyExistsException;
import code.model.Location;
import code.model.Rider;

import java.util.HashMap;
import java.util.Map;

public class RidersManager {

    private final Map<Integer, Rider> riders;

    public RidersManager() {
        this.riders = new HashMap<>();
    }

    public void registerRider(String name){
        Rider rider = new Rider(name);
        if(riders.containsKey(rider.getId())){
            throw new RidersAlreadyExistsException("rider with id: "+ rider.getId() + "already exists");
        }
        riders.put(rider.getId(), rider);
    }

    public void initiateBooking(Location from, Location to){

    }

    public boolean book(int bookingId){
        return true;
    }
}
