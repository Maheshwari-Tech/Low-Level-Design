package code.creational.strategy.vehicleExample.good;

public class Vehicle {
    IDriveStrategy driveStrategy;

    public Vehicle(IDriveStrategy driveStrategy){
        this.driveStrategy = driveStrategy;
    }
    public void driving(){
        driveStrategy.drive();
    }
}