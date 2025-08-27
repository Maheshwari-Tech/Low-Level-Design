package code.model;

public class Cab {
    private final String licenceNo;
    private String driverName;
    private Location currentLocation;
    private boolean isActive;

    public Cab(String licenceNo){
        this.licenceNo = licenceNo;
    }
    public Cab(String licenceNo, String driverName){
        this(licenceNo);
        this.driverName = driverName;
    }

    public String getLicenceNo() {
        return licenceNo;
    }

    public void setDriverName(String name){
        this.driverName = name;
    }

    public String getDriverName() {
        return driverName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
}
