package org.microsoft;

public class ProducerImpl implements Producer{
    private final Storage storage;
    public ProducerImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Status publish(Message message, int partition_id) {
        storage.addMessage(message, partition_id);
        return Status.SUCCESS;
    }

    @Override
    public int totalPartitions() {
        return storage.totalPartitions();
    }

}
