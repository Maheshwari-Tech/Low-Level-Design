package com.example.lld.product_catalog_service.port;

import com.example.lld.product_catalog_service.model.Category;
import com.example.lld.product_catalog_service.model.CategoryId;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category insert(Category category);

    Category replace(Category category, long expectedSchemaVersion);

    Optional<Category> findById(CategoryId categoryId);

    List<Category> findAll();
}
