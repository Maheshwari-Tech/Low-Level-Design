package com.mycompany.app.policies;

public class LRUBasedEvictionPolicy<Key> implements EvictionPolicy<Key> {

    @Override
    public Key keyToEvict() {
        return null;
    }

    @Override
    public void accessedKey(Key k) {

    }

    @Override
    public void delete(Key k) {

    }
}
