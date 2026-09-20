package com.example.lld.library_management_system;

public final class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final int totalCopies;

    private int availableCopies;

    public Book(String isbn, String title, String author, int totalCopies) {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author is required");
        }
        if (totalCopies <= 0) {
            throw new IllegalArgumentException("A book must have at least one copy");
        }
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void lendCopy() {
        if (!isAvailable()) {
            throw new IllegalStateException("No copy is available for ISBN " + isbn);
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies == totalCopies) {
            throw new IllegalStateException("All copies are already present for ISBN " + isbn);
        }
        availableCopies++;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }
}
