package code.behavioral.strategy.vehicleExample.good;

public class SportsVechile extends Vehicle{
    public SportsVechile() {
        super(new SpecialDrive());
    }
}
