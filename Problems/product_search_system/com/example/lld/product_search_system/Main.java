package com.example.lld.product_search_system;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SearchService searchService = new SearchService();

        // Add sample products
        searchService.addProduct(new Product("1", "iPhone 15", "Electronics",
                999.99, Arrays.asList("smartphone", "apple", "mobile")));
        searchService.addProduct(new Product("2", "MacBook Pro", "Electronics",
                1999.99, Arrays.asList("laptop", "apple", "computer")));
        searchService.addProduct(new Product("3", "Nike Air Max", "Clothing",
                129.99, Arrays.asList("shoes", "sports", "nike")));
        searchService.addProduct(new Product("4", "Samsung TV", "Electronics",
                799.99, Arrays.asList("tv", "samsung", "home")));
        searchService.addProduct(new Product("5", "Levi's Jeans", "Clothing",
                89.99, Arrays.asList("jeans", "levis", "casual")));

        System.out.println("=== Product Search System Demo ===\n");

        // Search by name
        System.out.println("Search by name 'phone':");
        List<Product> results = searchService.searchByName("phone");
        results.forEach(System.out::println);
        System.out.println();

        // Filter by category
        System.out.println("Filter by category 'Electronics':");
        results = searchService.filterByCategory("Electronics");
        results.forEach(System.out::println);
        System.out.println();

        // Filter by price range
        System.out.println("Filter by price range $100-$1000:");
        results = searchService.filterByPriceRange(100, 1000);
        results.forEach(System.out::println);
        System.out.println();

        // Search by tag
        System.out.println("Search by tag 'apple':");
        results = searchService.searchByTag("apple");
        results.forEach(System.out::println);
        System.out.println();

        // Advanced search
        System.out.println("Advanced search: Electronics, $500-$2000, sorted by price:");
        results = searchService.advancedSearch(null, "Electronics",
                500.0, 2000.0, null, "price");
        results.forEach(System.out::println);
    }
}
