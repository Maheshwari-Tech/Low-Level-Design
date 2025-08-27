package com.mycompany.app;

import com.mycompany.app.policies.LFUBasedEvictionPolicy;
import com.mycompany.app.policies.LRUBasedEvictionPolicy;
import com.mycompany.app.policies.RandomEvictionPolicy;
import com.mycompany.app.storage.HashMapBasedStorage;


import java.util.HashMap;

public class CacheFactory<Key,Value>{

    public Cache<Key, Value> defaultCache(final int capacity){
        return new CacheImpl<>(
                new HashMapBasedStorage<Key, Value>(capacity),
                new LRUBasedEvictionPolicy<Key>()
        );
    }

    public Cache<Key, Value> getSpaceOptimizedCache(final int capacity){
        return new CacheImpl<>(
                new HashMapBasedStorage<Key, Value>(capacity),
                new RandomEvictionPolicy<>()
        );
    }

    public Cache<Key, Value> getAllTimePopularCache(final int capacity){
        return new CacheImpl<>(
                new HashMapBasedStorage<Key, Value>(capacity),
                new LFUBasedEvictionPolicy<>()
        );
    }
}
