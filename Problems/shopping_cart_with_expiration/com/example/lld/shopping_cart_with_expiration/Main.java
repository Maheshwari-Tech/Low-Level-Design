package com.example.lld.shopping_cart_with_expiration;

import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ShoppingCart cart = new ShoppingCart();

        // Add items with different TTLs
        cart.addItem("1", "iPhone", 999.99, 5000); // 5 seconds
        cart.addItem("2", "Headphones", 199.99, 10000); // 10 seconds
        cart.addItem("3", "Charger", 49.99, 3000); // 3 seconds

        System.out.println("=== Shopping Cart with Expiration ===\n");

        System.out.println("Initial cart:");
        List<CartItem> contents = cart.getContents();
        contents.forEach(System.out::println);
        System.out.println("Total: $" + cart.getTotalPrice());
        System.out.println();

        // Wait for some items to expire
        Thread.sleep(4000);

        System.out.println("After 4 seconds:");
        contents = cart.getContents();
        contents.forEach(System.out::println);
        System.out.println("Total: $" + cart.getTotalPrice());
        System.out.println();

        // Wait more
        Thread.sleep(3000);

        System.out.println("After 7 seconds:");
        contents = cart.getContents();
        contents.forEach(System.out::println);
        System.out.println("Total: $" + cart.getTotalPrice());
    }
}
