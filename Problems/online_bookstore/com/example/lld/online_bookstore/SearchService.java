package com.example.lld.online_bookstore;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class SearchService {
    private Map<String, List<Book>> titleIndex;
    private Map<String, List<Book>> authorIndex;
    private Map<String, List<Book>> subjectIndex;

    public SearchService() {
        titleIndex = new HashMap<>();
        authorIndex = new HashMap<>();
        subjectIndex = new HashMap<>();
    }

    public void addBook(Book book) {
        addToIndex(titleIndex, book.getTitle().toLowerCase(), book);
        addToIndex(authorIndex, book.getAuthor().toLowerCase(), book);
        addToIndex(subjectIndex, book.getSubject().toLowerCase(), book);
    }

    private void addToIndex(Map<String, List<Book>> index, String key, Book book) {
        index.computeIfAbsent(key, k -> new ArrayList<>()).add(book);
    }

    public List<Book> searchByTitle(String title) {
        return titleIndex.getOrDefault(title.toLowerCase(), new ArrayList<>());
    }

    public List<Book> searchByAuthor(String author) {
        return authorIndex.getOrDefault(author.toLowerCase(), new ArrayList<>());
    }

    public List<Book> searchBySubject(String subject) {
        return subjectIndex.getOrDefault(subject.toLowerCase(), new ArrayList<>());
    }
}
