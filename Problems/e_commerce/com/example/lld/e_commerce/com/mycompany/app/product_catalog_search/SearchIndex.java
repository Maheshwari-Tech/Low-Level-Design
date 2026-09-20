package com.mycompany.app.product_catalog_search;

import java.util.*;
import java.util.stream.Collectors;

public class SearchIndex {
    private final Map<String, Set<Product>> nameIndex = new HashMap<>();
    private final Map<Category, Set<Product>> categoryIndex = new HashMap<>();
    private final Map<String, Set<Product>> tagIndex = new HashMap<>();
    private final NavigableMap<Double, Set<Product>> priceIndex = new TreeMap<>();

    public void addProduct(Product p) {
        // add to nameIndex (inverted index / trie)
        for (String token : p.getName().toLowerCase().split(" ")) {
            nameIndex.computeIfAbsent(token, k -> new HashSet<>()).add(p);
        }

        // category
        categoryIndex.computeIfAbsent(p.getCategory(), k -> new HashSet<>()).add(p);

        // tags
        for (String tag : p.getTags())
            tagIndex.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(p);

        // price
        priceIndex.computeIfAbsent(p.getPrice(), k -> new HashSet<>()).add(p);
    }

    public Set<Product> getByName(String query) {
        // partial match → check tokens starting with query
        return nameIndex.entrySet().stream()
                .filter(e -> e.getKey().startsWith(query.toLowerCase()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());
    }

    public Set<Product> getByCategory(Category category) {
        return categoryIndex.getOrDefault(category, Collections.emptySet());
    }

    public Set<Product> getByTag(String tag) {
        return tagIndex.getOrDefault(tag.toLowerCase(), Collections.emptySet());
    }

    public Set<Product> getByPriceRange(Double min, Double max) {
        return priceIndex.subMap(
                min != null ? min : priceIndex.firstKey(),
                true,
                max != null ? max : priceIndex.lastKey(),
                true
        ).values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    public Set<Product> getAllProducts() {
        return nameIndex.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }
}
