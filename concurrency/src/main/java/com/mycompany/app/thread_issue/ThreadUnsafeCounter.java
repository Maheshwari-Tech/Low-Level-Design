package com.mycompany.app.thread_issue;

public class ThreadUnsafeCounter {
    int count = 0;

    public void increment(){
        count++;
    }
    public void decrement(){
        count--;
    }

    void print(){
        System.out.println(" count is" + count);
    }
}
