public interface IStorage<Key, Value> {
    void delete(Key key) throws KeyNotFoundException;
    Value get(Key key) throws KeyNotFoundException;
    void add(Key key, Value value) throws StorageFullException;
    boolean isFull();
}
