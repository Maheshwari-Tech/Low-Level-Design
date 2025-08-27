package com.mycompany.app.model;

public class Location {
    private final int x;
    private final int y;

    Location(final int x, final int y){
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
