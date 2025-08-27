package code.policies;

public interface EvictionPolicy<Key> {
    Key keyToEvict();
    void accessedKey(Key k);
    void delete(Key k);
}
