package org.microsoft;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class LocalStorage implements Storage {

    Map<Integer, PriorityQueue<Message>> messages = new HashMap<>();

    @Override
    public synchronized void addMessage(Message message, int partition_id) {
        messages.computeIfAbsent(partition_id, k -> new PriorityQueue<>()).add(message);
    }

    @Override
    public synchronized void removeMessage(int partition_id) {
        if(!messages.containsKey(partition_id)){
            throw new IllegalArgumentException("No such partition");
        }
        messages.get(partition_id).poll();
    }

    @Override
    public synchronized Message getMessage(int partition_id) {
        if(!messages.containsKey(partition_id)){
            throw new IllegalArgumentException("No such partition");
        }
       return messages.get(partition_id).peek();
    }

    @Override
    public int totalPartitions() {
        return messages.size();
    }


}
