package org.microsoft;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceImpl {
    int N;
    Producer producer;
    Consumer consumer;
    Map<Integer, ExecutorService> map = new HashMap<>();

    ServiceImpl(Producer producer, Consumer consumer, int N){
        this.producer = producer;
        this.consumer = consumer;
        this.N = N;
        for(int i = 0; i < N; i++){
            map.put(i, Executors.newSingleThreadExecutor());
        }
    }

    // TODO : seperate class to have responsibilty of generating hash
    int gethash(int key){
        return key%N;
    }


    public void consume(int key) {
        map.get(gethash(key)).execute(() -> consumer.consume(key));
    }

    public void produce(Message message, int key){
        map.get(gethash(key)).execute(() -> producer.publish(message, key));
    }
}
