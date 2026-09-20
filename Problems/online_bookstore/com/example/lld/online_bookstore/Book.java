package com.example.lld.online_bookstore;

public class Book {
    private String isbn;
    private String title;
    private String author;
    private String subject;
    private double price;
    private int stock;

    public Book(String isbn, String title, String author, String subject, double price, int stock) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.subject = subject;
        this.price = price;
        this.stock = stock;
    }

    // Getters and setters
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSubject() { return subject; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", subject='" + subject + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                '}';
    }
}
