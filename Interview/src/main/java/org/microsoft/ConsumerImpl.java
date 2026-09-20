package org.microsoft;

public class ConsumerImpl implements Consumer {

    Storage storage;
    public ConsumerImpl(Storage storage) {
        this.storage = storage;
    }
    @Override
    public Status consume(int partition_id) {
        Message message = storage.getMessage(partition_id);
        System.out.println(message);
        storage.removeMessage(partition_id);
        return Status.SUCCESS;
    }
}
