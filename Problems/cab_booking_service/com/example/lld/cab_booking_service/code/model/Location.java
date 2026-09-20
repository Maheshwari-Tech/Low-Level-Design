package code.model;

public class Location {
    private final double x;
    private final double y;

    Location(final double x, final double y){
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
