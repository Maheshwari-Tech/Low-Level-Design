package com.mycompany.app.thread_issue;

import java.util.Random;

public class ThreadUnsafeCounterDemo {

    static Random random = new Random(System.currentTimeMillis());

    public static void main(String[] args) throws InterruptedException {

        ThreadUnsafeCounter badCounter = new ThreadUnsafeCounter();

        Thread t1 = new Thread(() -> {
            for(int i=0; i<100; ++i){
                badCounter.increment();
                sleepRandomlyForLessThan10Secs();
            };
        });

        Thread t2 = new Thread(() -> {
            for(int i=0; i<100; ++i){
                badCounter.decrement();
                sleepRandomlyForLessThan10Secs();
            };
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        badCounter.print();
    }
    static void sleepRandomlyForLessThan10Secs(){
        try{
            Thread.sleep(random.nextInt(10));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
