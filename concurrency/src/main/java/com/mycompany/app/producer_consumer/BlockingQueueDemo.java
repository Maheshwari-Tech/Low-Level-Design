package com.mycompany.app.producer_consumer;

public class BlockingQueueDemo {

    public static void test1() throws InterruptedException {
        final BockingQueue<Integer> q = new BlockingQueueWithSemaphoreImpl<>(5);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; ++i) {
                try {
                    q.enqueue(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("added: " + i);
            }
        });

        Thread t2 = new Thread(() -> {
            int item;

            for (int i = 0; i < 25; ++i) {
                try {
                    item = q.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("removed: " + item);
            }
        });


        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 25; ++i) {
                int item;
                try {
                    item = q.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("removed: " + item);
            }
        });

        t1.start();
        Thread.sleep(4000);
        t2.start();
        t3.start();

        t2.join();
        t1.join();
        t3.join();
    }


    public static void main(String[] args) throws InterruptedException {
        test1();
    }
}
