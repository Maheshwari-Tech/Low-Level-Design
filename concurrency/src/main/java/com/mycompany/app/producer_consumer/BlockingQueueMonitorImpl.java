package com.mycompany.app.producer_consumer;

public class BlockingQueueMonitorImpl<T> implements BockingQueue<T>{

    T[] array;
    int size = 0;
    int capacity;
    int head = 0;
    int tail = 0;

    BlockingQueueMonitorImpl(int capacity){
        this.capacity = capacity;
        array = (T[]) new Object[capacity];
    }

        @Override
    public synchronized void enqueue(T item) throws InterruptedException {
        while(size == capacity){
            wait();
        }
        if(tail == capacity){
            tail = 0;
        }
        array[tail] = item;
        size++;
        tail++;
        notifyAll();
    }

    @Override
    public synchronized T dequeue() throws InterruptedException {
        while(size == 0){
            wait();
        }
        T item;

        if(head == capacity){
            head = 0;
        }
        item = array[head];
        array[head] = null;
        head++;
        size--;
        notifyAll();
        return item;
    }

}
