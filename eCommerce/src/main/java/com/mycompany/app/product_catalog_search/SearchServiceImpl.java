package com.mycompany.app.product_catalog_search;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchServiceImpl implements SearchService {
    private final SearchIndex index;

    public SearchServiceImpl(SearchIndex index) {
        this.index = index;
    }


    @Override
    public List<Product> search(SearchQuery query) {
        Set<Product> results = new HashSet<>(index.getAllProducts());
        if(query.getCategory() != null){
            results.retainAll(index.getByCategory(query.getCategory()));
        }
        if(query.getMinPrice() != null || query.getMaxPrice() != null){
            results.retainAll(index.getByPriceRange(query.getMinPrice(), query.getMaxPrice()));
        }
        if(query.getTags() != null){
            for(String tag: query.getTags()){
                results.retainAll(index.getByTag(tag));
            }
        }
        return sorted(List.copyOf(results), query);
    }

    private List<Product> sorted(List<Product> products, SearchQuery searchQuery){
        SortType sortType = searchQuery.getSortBy();
        if(sortType == null) return products;
        switch (sortType){
            case PRICE_ASC -> products.sort(Comparator.comparing(Product::getPrice));
            case PRICE_DESC -> products.sort(Comparator.comparing(Product::getPrice).reversed());
            case RELEVANCE -> products.sort(new RelavanceComparator(searchQuery));
            default -> {
                return products;
            }
        }
        return null;
    }
}
