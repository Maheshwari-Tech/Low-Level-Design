package code;

import code.policies.LFUBasedEvictionPolicy;
import code.policies.LRUBasedEvictionPolicy;
import code.policies.RandomEvictionPolicy;
import code.storage.HashMapBasedStorage;

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
