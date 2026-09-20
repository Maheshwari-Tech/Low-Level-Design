package code.policies;

import code.algorithms.DoublyLinkedList;
import code.algorithms.DoublyLinkedListNode;
import code.exception.NotFoundException;

import java.util.HashMap;
import java.util.Map;

public class LRUBasedEvictionPolicy<Key> implements EvictionPolicy<Key> {
    Map<Key, DoublyLinkedListNode<Key>> dllNodeMap = new HashMap<>();
    DoublyLinkedList<Key> dll = new DoublyLinkedList<>();

    @Override
    public Key keyToEvict() {
        if(dll.isEmpty()){
            throw new NotFoundException("No key found to evict");
        }
        return dll.getTail().getItem();
    }

    @Override
    public void accessedKey(Key k) {
        if(dllNodeMap.containsKey(k)){
           delete(k);
        }
        dll.addInBegin(k);
        dllNodeMap.put(k, dll.getHead());
    }

    @Override
    public void delete(Key k) {
        if(!dllNodeMap.containsKey(k)){
           throw new NotFoundException("key doesn't exists");
        }
        dll.deleteNode(dllNodeMap.get(k));
        dllNodeMap.remove(k);
    }
}
