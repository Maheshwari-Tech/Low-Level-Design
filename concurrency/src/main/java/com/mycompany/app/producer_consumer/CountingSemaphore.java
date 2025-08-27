package com.mycompany.app.producer_consumer;

public class CountingSemaphore {
    int usedPermit = 0;
    int maxCount;

    CountingSemaphore(int count, int initialPermit){
        this.maxCount = count;
        this.usedPermit = this.maxCount - initialPermit;
    }

    public synchronized void acquire() throws InterruptedException {

        while(usedPermit == maxCount){
            wait();
        }
        usedPermit++;
        notify();
    }

    public synchronized void release() throws InterruptedException {
        while(usedPermit == 0){
            wait();
        }
        usedPermit--;
        notify();
    }
}


