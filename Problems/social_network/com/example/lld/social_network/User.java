package com.example.lld.social_network;

import java.util.HashSet;
import java.util.Set;

public class User {
    private String id;
    private String name;
    private Set<String> friends;
    private Set<Post> posts;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.friends = new HashSet<>();
        this.posts = new HashSet<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Set<String> getFriends() { return friends; }
    public Set<Post> getPosts() { return posts; }

    public void addFriend(String friendId) {
        friends.add(friendId);
    }

    public void removeFriend(String friendId) {
        friends.remove(friendId);
    }

    public void addPost(Post post) {
        posts.add(post);
    }

    public boolean isFriend(String userId) {
        return friends.contains(userId);
    }
}
