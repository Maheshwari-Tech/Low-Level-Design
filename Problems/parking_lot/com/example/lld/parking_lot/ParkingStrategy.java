package com.example.lld.parking_lot;

public interface ParkingStrategy {
    void reset();

    void addId(int id);

    int getNextSlot();

    void removeId(int id);
}
