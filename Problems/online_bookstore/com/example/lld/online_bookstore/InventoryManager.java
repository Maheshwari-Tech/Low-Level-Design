package com.example.lld.online_bookstore;

import java.util.Map;
import java.util.HashMap;

public class InventoryManager {
    private Map<String, Book> inventory; // ISBN to Book

    public InventoryManager() {
        inventory = new HashMap<>();
    }

    public void addBook(Book book) {
        inventory.put(book.getIsbn(), book);
    }

    public Book getBook(String isbn) {
        return inventory.get(isbn);
    }

    public boolean isInStock(String isbn, int quantity) {
        Book book = inventory.get(isbn);
        return book != null && book.getStock() >= quantity;
    }

    public void updateStock(String isbn, int quantity) {
        Book book = inventory.get(isbn);
        if (book != null) {
            book.setStock(book.getStock() - quantity);
        }
    }

    public void restock(String isbn, int quantity) {
        Book book = inventory.get(isbn);
        if (book != null) {
            book.setStock(book.getStock() + quantity);
        }
    }

    public Map<String, Book> getAllBooks() {
        return inventory;
    }
}
