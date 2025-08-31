package com.mycompany.app.models;

import java.util.concurrent.atomic.AtomicInteger;

public class Screens {
    private final int id;
    private final int theatreId;
    private String displayName;
    private static volatile AtomicInteger counter = new AtomicInteger();

    public Screens(int theatreId, String displayName) {
        this.id = counter.getAndIncrement();
        this.theatreId = theatreId;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public int getTheatreId() {
        return theatreId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
