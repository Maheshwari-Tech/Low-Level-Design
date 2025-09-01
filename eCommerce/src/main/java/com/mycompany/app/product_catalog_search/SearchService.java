package com.mycompany.app.product_catalog_search;

import java.util.List;

public interface SearchService {
    List<Product> search(SearchQuery query);
}
