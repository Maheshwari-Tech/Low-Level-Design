package com.example.lld.online_bookstore;

public class Main {
    public static void main(String[] args) {
        // Initialize components
        InventoryManager inventory = new InventoryManager();
        SearchService searchService = new SearchService();
        OrderProcessor orderProcessor = new OrderProcessor(inventory);

        // Add books
        Book book1 = new Book("123", "Java Basics", "Author A", "Programming", 29.99, 10);
        Book book2 = new Book("456", "Advanced Java", "Author A", "Programming", 39.99, 5);
        Book book3 = new Book("789", "Data Structures", "Author B", "CS", 49.99, 8);

        inventory.addBook(book1);
        inventory.addBook(book2);
        inventory.addBook(book3);

        searchService.addBook(book1);
        searchService.addBook(book2);
        searchService.addBook(book3);

        // Create patron
        Patron patron = new Patron("P1", "John Doe", "john@example.com");

        // Search books
        System.out.println("Books by Author A: " + searchService.searchByAuthor("Author A"));

        // Place order
        Order order = new Order("O1", patron);
        order.addItem(book1, 2);
        order.addItem(book3, 1);

        if (orderProcessor.placeOrder(order)) {
            System.out.println("Order placed successfully. Total: $" + order.getTotalAmount());
            patron.addOrder(order);
        } else {
            System.out.println("Failed to place order due to insufficient stock.");
        }

        // Check inventory
        System.out.println("Remaining stock for Java Basics: " + book1.getStock());
    }
}
