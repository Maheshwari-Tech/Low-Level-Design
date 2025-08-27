package com.mycompany.app;

import com.mycompany.app.policies.EvictionPolicy;
import com.mycompany.app.storage.Storage;

public class CacheImpl<Key, Value> implements Cache<Key, Value> {

    Storage<Key, Value> storage;
    EvictionPolicy<Key> evictionPolicy;

    CacheImpl(Storage<Key, Value> storage, EvictionPolicy<Key> evictionPolicy){
        this.storage = storage;
        this.evictionPolicy = evictionPolicy;
    }

    @Override
    public void put(Key k, Value v) {
        if(!storage.isPresent(k) && storage.isFull()){
           Key evictedKey = evictionPolicy.keyToEvict();
           storage.delete(evictedKey);
           evictionPolicy.delete(evictedKey);
        }
        storage.add(k, v);
        evictionPolicy.accessedKey(k);
    }

    @Override
    public Value get(Key k) {
        if(storage.isPresent(k)){
            evictionPolicy.accessedKey(k);
            return storage.fetch(k);
        }
        return null;
    }

    @Override
    public void delete(Key k) {
       if(storage.isPresent(k)){
           evictionPolicy.delete(k);
           storage.delete(k);
       }
    }

    @Override
    public int size() {
        return storage.getSize();
    }
}
