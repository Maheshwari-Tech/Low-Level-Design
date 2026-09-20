package com.mycompany.app.models;

public class Seat {
    private final int screenId;
    private final int id;
    private final String displayName;

    public Seat(int screenId, int id, String displayName) {
        this.screenId = screenId;
        this.id = id;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScreenId() {
        return screenId;
    }

    public int getId() {
        return id;
    }
}
