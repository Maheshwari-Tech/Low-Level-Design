package code.producer_consumer;

public class BlockingQueueWithLocksFaultyImpl<T> implements BockingQueue<T>{
    @Override
    public void enqueue(T item) throws InterruptedException {

    }

    @Override
    public T dequeue() throws InterruptedException {
        return null;
    }
}
