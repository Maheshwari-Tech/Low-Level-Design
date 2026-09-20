public class CacheFactory {
    ICache getDefaultCache(int N){
        return new CacheImpl(new HashMapBasedStorage<>(N), new LRUEvictionPolicy<>());
    }
}
