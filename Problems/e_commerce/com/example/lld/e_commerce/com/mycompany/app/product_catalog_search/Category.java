package com.mycompany.app.product_catalog_search;

import java.util.Arrays;

public enum Category {
    BOOKS("books"), ELECTRONICS("electronics");

    private String name;

    Category(String name){
        this.name = name;
    }

    public static Category from(final String category) {
        return Arrays.stream(values())
                .filter(c -> c.name.equalsIgnoreCase(category))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown category: " + category));
    }
}
