package com.mycompany.app.models;

import java.time.LocalDateTime;

public class Booking {
    private final int id;
    private final int showId;
    private final int seatId;
    private final int userId;
    private BookingStatus status;
    private final LocalDateTime timestamp;

    public Booking(int id, int showId, int seatId, int userId) {
        this.id = id;
        this.showId = showId;
        this.seatId = seatId;
        this.userId = userId;
        this.status = BookingStatus.UNKNOWN;
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getId() {
        return id;
    }

    public int getSeatId() {
        return seatId;
    }

    public int getShowId() {
        return showId;
    }

    public int getUserId() {
        return userId;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
