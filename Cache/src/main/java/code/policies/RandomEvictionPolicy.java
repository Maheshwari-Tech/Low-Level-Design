package code.policies;

public class RandomEvictionPolicy<Key> implements EvictionPolicy<Key> {

    @Override
    public Key keyToEvict() {
        return null;
    }

    @Override
    public void accessedKey(Key k) {
        // do nothing.
    }

    @Override
    public void delete(Key k) {

    }

}
