package com.example.lld.social_network;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        SocialNetwork network = new SocialNetwork();

        // Add users
        network.addUser("1", "Alice");
        network.addUser("2", "Bob");
        network.addUser("3", "Charlie");

        // Add friendships
        network.addFriend("1", "2");
        network.addFriend("2", "3");

        // Create posts
        network.createPost("p1", "1", "Hello world!");
        network.createPost("p2", "2", "Nice day!");
        network.createPost("p3", "3", "Coding is fun");
        network.createPost("p4", "1", "Another post");

        // Like some posts
        network.likePost("p1");
        network.likePost("p1");
        network.likePost("p2");

        System.out.println("=== Social Network Demo ===\n");

        // Get news feed for Alice
        System.out.println("Alice's news feed:");
        List<Post> feed = network.getNewsFeed("1");
        for (Post post : feed) {
            System.out.println(post);
        }

        System.out.println("\nBob's news feed:");
        feed = network.getNewsFeed("2");
        for (Post post : feed) {
            System.out.println(post);
        }
    }
}
