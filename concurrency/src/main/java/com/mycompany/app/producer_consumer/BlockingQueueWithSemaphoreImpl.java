package com.mycompany.app.producer_consumer;

public class BlockingQueueWithSemaphoreImpl<T> implements BockingQueue<T> {

    T[] array;
    int size = 0;
    int capacity;
    int head = 0;
    int tail = 0;

    CountingSemaphore semLock = new CountingSemaphore(1, 1);

    CountingSemaphore semProducer;
    CountingSemaphore semConsumer;




    BlockingQueueWithSemaphoreImpl(int capacity){
        this.capacity = capacity;
        array = (T[]) new Object[capacity];

        semProducer = new CountingSemaphore(capacity, capacity);
        semConsumer = new CountingSemaphore(capacity, 0);

    }

    public  void enqueue(T item) throws InterruptedException {
        semProducer.acquire();
        semLock.acquire();

        if(tail == capacity){
            tail = 0;
        }
        array[tail] = item;
        size++;
        tail++;
        semConsumer.release();
        semLock.release();
    }

    public T dequeue() throws InterruptedException {

        semConsumer.acquire();
        semLock.acquire();
        T item;

        if(head == capacity){
            head = 0;
        }
        item = array[head];
        array[head] = null;
        head++;
        size--;

        semProducer.release();
        semLock.release();
        return item;
    }
}
