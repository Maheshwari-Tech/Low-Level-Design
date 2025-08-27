package com.mycompany.app.storage;

public interface Storage<Key, Value> {

    void add(Key k, Value v);

    void delete(Key k);

    Value fetch(Key k);

    boolean isPresent(Key k);

    boolean isFull();

    int getSize();
}
