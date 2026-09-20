package code.producer_consumer;

public interface BockingQueue<T> {
    void enqueue(T item) throws InterruptedException;
    T dequeue() throws InterruptedException;
}
