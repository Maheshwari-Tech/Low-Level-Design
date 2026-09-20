import java.util.HashMap;
import java.util.Map;

public class HashMapBasedStorage<Key, Value> implements IStorage<Key, Value> {

    private Map<Key, Value> mp;
    private int maxCapacity;
    private int currentSize;

    HashMapBasedStorage(int N){
        maxCapacity = N;
        currentSize = 0;
        mp = new HashMap<>();
    }

    @Override
    public Value get(Key key) throws KeyNotFoundException {
        if(mp.containsKey(key)){
            return mp.get(key);
        }
        else{
            throw new KeyNotFoundException();
        }
    }

    @Override
    public void add(Key key, Value value) throws StorageFullException {
        if(currentSize == maxCapacity){
            throw new StorageFullException();
        }
        else{
            if(!mp.containsKey(key)){
                currentSize++;
            }
            mp.put(key, value);
        }
    }

    @Override
    public void delete(Key key) throws KeyNotFoundException {
        if(!mp.containsKey(key)){
            throw new KeyNotFoundException();
        }
        mp.remove(key);
    }

    @Override
    public boolean isFull() {
        return maxCapacity == currentSize;
    }
}
