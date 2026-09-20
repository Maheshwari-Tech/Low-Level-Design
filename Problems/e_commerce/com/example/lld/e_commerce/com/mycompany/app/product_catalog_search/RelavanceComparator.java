package com.mycompany.app.product_catalog_search;

import java.util.Comparator;

public class RelavanceComparator implements Comparator<Product> {

    private final SearchQuery query;

    public RelavanceComparator(SearchQuery query) {
        this.query = query;
    }

    @Override
    public int compare(Product o1, Product o2) {
        return 0;
    }

}
