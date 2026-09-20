package org.microsoft;

import java.security.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

public class Message {
    String message;
    Timestamp timestamp;
    private static final AtomicInteger counter = new AtomicInteger(0);
    int id;

    public Message(String message, Timestamp timestamp, int id) {
        this.message = message;
        this.timestamp = timestamp;
        this.id = counter.incrementAndGet();
    }
}
