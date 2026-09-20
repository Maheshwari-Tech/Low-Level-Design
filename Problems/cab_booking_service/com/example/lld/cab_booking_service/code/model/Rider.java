package code.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Rider {
     private String name;
     private final int id;
     private final static AtomicInteger counter = new AtomicInteger();

     public Rider(String name){
         this.name = name;
         this.id = counter.incrementAndGet();
     }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
