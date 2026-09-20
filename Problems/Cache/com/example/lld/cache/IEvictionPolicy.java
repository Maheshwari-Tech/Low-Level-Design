import java.util.Optional;

public interface IEvictionPolicy<Key> {

    Optional<Key> evictKey();
    void acessedKey(Key key);
}
