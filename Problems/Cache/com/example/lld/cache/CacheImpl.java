import java.util.Optional;

public class CacheImpl<Key, Value> implements ICache<Key, Value>{

    IStorage storage;
    IEvictionPolicy evictionPolicy;

    CacheImpl(IStorage storage, IEvictionPolicy evictionPolicy){
        this.storage = storage;
        this.evictionPolicy = evictionPolicy;
    }

    @Override
    public Value get(Key key) throws KeyNotFoundException {
        evictionPolicy.acessedKey(key);
        return (Value) storage.get(key);
    }

    @Override
    public void put(Key key, Value value) {
        try{
            storage.add(key, value);
            evictionPolicy.acessedKey(key);
        } catch (StorageFullException e) {
            Optional<Key> optionalKey = evictionPolicy.evictKey();
            if(!optionalKey.isPresent()){
                throw new RuntimeException();
            }
            try {
                storage.delete(key);
                put(key, value);
            } catch (KeyNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
