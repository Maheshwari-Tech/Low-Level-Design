package com.mycompany.app.product_catalog_search;

import java.util.List;

public class SearchQuery {
    private final String name;
    private final Category category;
    private final Double minPrice;
    private final Double maxPrice;
    private final List<String> tags;
    private final SortType sortBy;

    public List<String> getTags() {
        return tags;
    }

    public String getName() {
        return name;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public SortType getSortBy() {
        return sortBy;
    }

    public Category getCategory() {
        return category;
    }

    private SearchQuery(String name, Category category, Double minPrice, Double maxPrice, List<String>tags, SortType sortBy){
        this.name = name;
        this.category = category;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.sortBy = sortBy;
        this.tags = tags;
    }


    private static class SearchQueryBuilder{
        private String name;
        private Category category;
        private Double minPrice;
        private Double maxPrice;
        private List<String> tags;
        private SortType sortBy;

        public SearchQuery build(){
            return new SearchQuery(name, category, minPrice, maxPrice, tags, sortBy);
        }

        public void setName(String name){
            this.name = name;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public void setMaxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
        }

        public void setMinPrice(Double minPrice) {
            this.minPrice = minPrice;
        }

        public void setSortBy(SortType sortBy) {
            this.sortBy = sortBy;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
}
