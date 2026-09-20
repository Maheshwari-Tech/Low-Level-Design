package com.example.lld.product_search_system;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Product Search Service
 *
 * Design Choices:
 * - In-memory storage with List for simplicity.
 * - Stream API for filtering and sorting.
 * - Case-insensitive name search.
 * - Tag search with contains check.
 */
public class SearchService {
    private List<Product> products;

    public SearchService() {
        this.products = new ArrayList<>();
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    /**
     * Search by name (partial match, case-insensitive)
     */
    public List<Product> searchByName(String name) {
        return products.stream()
                .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Filter by category
     */
    public List<Product> filterByCategory(String category) {
        return products.stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Filter by price range
     */
    public List<Product> filterByPriceRange(double minPrice, double maxPrice) {
        return products.stream()
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    /**
     * Search by tags
     */
    public List<Product> searchByTag(String tag) {
        return products.stream()
                .filter(p -> p.getTags().stream()
                        .anyMatch(t -> t.equalsIgnoreCase(tag)))
                .collect(Collectors.toList());
    }

    /**
     * Sort by price
     */
    public List<Product> sortByPrice(List<Product> productList, boolean ascending) {
        return productList.stream()
                .sorted(ascending ?
                        Comparator.comparingDouble(Product::getPrice) :
                        Comparator.comparingDouble(Product::getPrice).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Combined search with multiple filters
     */
    public List<Product> advancedSearch(String name, String category,
                                      Double minPrice, Double maxPrice,
                                      String tag, String sortBy) {
        List<Product> results = new ArrayList<>(products);

        if (name != null && !name.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        if (minPrice != null && maxPrice != null) {
            results = results.stream()
                    .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        if (tag != null && !tag.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getTags().stream()
                            .anyMatch(t -> t.equalsIgnoreCase(tag)))
                    .collect(Collectors.toList());
        }

        if ("price".equalsIgnoreCase(sortBy)) {
            results = sortByPrice(results, true);
        }

        return results;
    }
}
