package code.producer_consumer;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueWithLockImpl<T> implements BockingQueue<T>{

    T[] array;
    int size = 0;
    int capacity;
    int head = 0;
    int tail = 0;

    Lock lock = new ReentrantLock();

    BlockingQueueWithLockImpl(int capacity){
        this.capacity = capacity;
        array = (T[]) new Object[capacity];
    }

    public void enqueue(T item) throws InterruptedException {
        lock.lock();

        while(size == capacity){
            lock.unlock(); //release the lock to give other thread a chance.

            lock.lock(); // require the lock before checking the condition.
        }

        if(tail == capacity){
            tail = 0;
        }
        array[tail] = item;
        size++;
        tail++;
        lock.unlock();
    }

    public  T dequeue() throws InterruptedException {
        lock.lock();

        while(size == 0){
            lock.unlock();
            lock.lock();
        }
        T item;

        if(head == capacity){
            head = 0;
        }
        item = array[head];
        array[head] = null;
        head++;
        size--;
        lock.unlock();
        return item;
    }
}

