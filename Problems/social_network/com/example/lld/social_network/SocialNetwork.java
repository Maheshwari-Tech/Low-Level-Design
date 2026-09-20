package com.example.lld.social_network;

import java.util.*;

/**
 * Social Network System
 *
 * Design Choices:
 * - HashMap for O(1) user lookups.
 * - Sets for friends and posts to avoid duplicates.
 * - News feed shows posts from user and friends, sorted by timestamp.
 */
public class SocialNetwork {
    private Map<String, User> users;
    private Map<String, Post> posts;

    public SocialNetwork() {
        this.users = new HashMap<>();
        this.posts = new HashMap<>();
    }

    public void addUser(String id, String name) {
        users.put(id, new User(id, name));
    }

    public void addFriend(String userId1, String userId2) {
        User user1 = users.get(userId1);
        User user2 = users.get(userId2);
        if (user1 != null && user2 != null) {
            user1.addFriend(userId2);
            user2.addFriend(userId1);
        }
    }

    public void createPost(String postId, String userId, String content) {
        User user = users.get(userId);
        if (user != null) {
            Post post = new Post(postId, userId, content);
            posts.put(postId, post);
            user.addPost(post);
        }
    }

    public void likePost(String postId) {
        Post post = posts.get(postId);
        if (post != null) {
            post.like();
        }
    }

    public List<Post> getNewsFeed(String userId) {
        User user = users.get(userId);
        if (user == null) return new ArrayList<>();

        Set<Post> feedPosts = new HashSet<>(user.getPosts());

        for (String friendId : user.getFriends()) {
            User friend = users.get(friendId);
            if (friend != null) {
                feedPosts.addAll(friend.getPosts());
            }
        }

        return feedPosts.stream()
                .sorted((p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()))
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    public Post getPost(String postId) {
        return posts.get(postId);
    }
}
