package com.mycompany.app;

public class SumUpExample {

    long start;
    long end;
    public long sum = 0;

    public SumUpExample(long start, long end){
        this.start = start;
        this.end = end;
    }

    public void add(){
        for(long i=start; i<=end; ++i){
            sum+=i;
        }
    }

    static void multithreading() throws InterruptedException{
        long start = System.currentTimeMillis();
        SumUpExample s1 = new SumUpExample(1, Integer.MAX_VALUE/2);
        SumUpExample s2 = new SumUpExample(Integer.MAX_VALUE/2 +1, Integer.MAX_VALUE);

        Thread t1 = new Thread(s1::add);
        Thread t2 = new Thread(s2::add);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        long finalCount = s1.sum + s2.sum;
        long end = System.currentTimeMillis();

        System.out.println("multithreading time : - " + (end-start));
        System.out.println("sum is : " + finalCount);

    }

    static void singleThread() throws InterruptedException{
        long start = System.currentTimeMillis();
        SumUpExample s1 = new SumUpExample(1, Integer.MAX_VALUE);
        s1.add();
        long end = System.currentTimeMillis();
        System.out.println("single time : - " + (end-start));
        System.out.println("sum is : " + s1.sum);
    }

    public static void main(String[] args) throws InterruptedException {
        singleThread();
        multithreading();
    }

}
