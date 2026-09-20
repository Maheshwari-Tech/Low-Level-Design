import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class LRUEvictionPolicy<Key> implements IEvictionPolicy<Key> {

    DoublyLinkedList<Key> dll;
    Map<Key, DoublyLinkedListNode<Key>> mp = new HashMap<>();


    @Override
    public Optional evictKey() {
        if(dll.isEmpty()) return Optional.empty();
        DoublyLinkedListNode doublyLinkedListNode = dll.removeFirstNode();
        return Optional.of(doublyLinkedListNode.getValue());
    }

    @Override
    public void acessedKey(Key key) {
        if(mp.containsKey(key)){
            mp.remove(key);
        }
        DoublyLinkedListNode doublyLinkedListNode = new DoublyLinkedListNode(key);
        dll.insertInEnd(doublyLinkedListNode);
        mp.put(key, doublyLinkedListNode);
    }
}
