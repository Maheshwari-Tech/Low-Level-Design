package org.microsoft;

public interface Storage {
    void addMessage(Message message, int partition_id);
    void removeMessage(int partition_id);
    Message getMessage(int partition_id);

    int totalPartitions();
}
