package com.example.lld.social_network;

public class Post {
    private String id;
    private String userId;
    private String content;
    private long timestamp;
    private int likes;

    public Post(String id, String userId, String content) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.likes = 0;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public int getLikes() { return likes; }

    public void like() {
        likes++;
    }

    @Override
    public String toString() {
        return userId + ": " + content + " (likes: " + likes + ")";
    }
}
