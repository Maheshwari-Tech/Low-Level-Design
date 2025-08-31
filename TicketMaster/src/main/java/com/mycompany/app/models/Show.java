package com.mycompany.app.models;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Show {
    private final int movieId;
    private final int screenId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int id;
    private static volatile AtomicInteger counter = new AtomicInteger();

    public Show(int movieId, int screenId, LocalDateTime startTime, LocalDateTime endTime) {
        this.movieId = movieId;
        this.screenId = screenId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = counter.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    public int getMovieId() {
        return movieId;
    }

    public int getScreenId() {
        return screenId;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}
