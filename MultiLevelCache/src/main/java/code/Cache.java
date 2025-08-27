package code;

public interface Cache<Key, Value> {
    void put(Key k, Value v);
    Value get(Key k);
    void delete(Key k);

    int size();
}
