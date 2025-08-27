package com.mycompany.app.storage;

import com.mycompany.app.exception.NotFoundException;
import com.mycompany.app.exception.StorageFullException;

import java.util.HashMap;
import java.util.Map;

public class HashMapBasedStorage<Key, Value> implements Storage<Key, Value> {

    int capacity;
    Map<Key, Value> mp;

    public HashMapBasedStorage(int capacity){
        this.capacity = capacity;
        mp = new HashMap<>();
    }

    @Override
    public void add(Key k, Value v) {
        if(isFull()){
            throw new StorageFullException("Capacity full");
        }
        mp.put(k, v);
    }

    @Override
    public void delete(Key k) {
        if(!isPresent(k)){
            throw new NotFoundException(k + " doesn't exist in cache");
        }
        mp.remove(k);
    }

    @Override
    public Value fetch(Key k) {
        if(!isPresent(k)){
            throw new NotFoundException(k + " doesn't exist in cache");
        }
        return mp.get(k);
    }

    @Override
    public boolean isPresent(Key k) {
        return mp.containsKey(k);
    }

    @Override
    public boolean isFull() {
        return capacity == mp.size();
    }

    @Override
    public int getSize() {
        return mp.size();
    }
}
