package com.mycompany.app.models;

import java.util.concurrent.atomic.AtomicInteger;

public class Movie {
    private final String name;
    private final String contentUrl;
    private final int id;
    private static volatile AtomicInteger counter = new AtomicInteger();
    private final int durationInMinute;

    public Movie(String name, String contentUrl, int durationInMinute) {
        this.name = name;
        this.contentUrl = contentUrl;
        this.durationInMinute = durationInMinute;
        this.id = counter.getAndIncrement();
    }

    public int getDurationInMinute() {
        return durationInMinute;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContentUrl() {
        return contentUrl;
    }
}
