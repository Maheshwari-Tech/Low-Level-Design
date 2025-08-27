package com.mycompany.app.policies;

import com.mycompany.app.algorithms.DoublyLinkedList;
import com.mycompany.app.algorithms.DoublyLinkedListNode;
import com.mycompany.app.exception.NotFoundException;

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
