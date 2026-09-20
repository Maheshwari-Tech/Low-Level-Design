package com.example.lld.product_catalog_service.service;

import com.example.lld.product_catalog_service.exception.CategoryAlreadyExistsException;
import com.example.lld.product_catalog_service.exception.CategoryNotFoundException;
import com.example.lld.product_catalog_service.exception.VersionConflictException;
import com.example.lld.product_catalog_service.model.Category;
import com.example.lld.product_catalog_service.model.CategoryId;
import com.example.lld.product_catalog_service.port.CategoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class InMemoryCategoryRepository implements CategoryRepository {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Map<CategoryId, Category> categories = new HashMap<>();

    @Override
    public Category insert(Category category) {
        lock.writeLock().lock();
        try {
            if (categories.containsKey(category.id())) {
                throw new CategoryAlreadyExistsException(category.id());
            }
            categories.put(category.id(), category);
            return category;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Category replace(Category category, long expectedSchemaVersion) {
        lock.writeLock().lock();
        try {
            Category current = categories.get(category.id());
            if (current == null) {
                throw new CategoryNotFoundException(category.id());
            }
            if (current.schemaVersion() != expectedSchemaVersion) {
                throw new VersionConflictException(
                        "category schema",
                        category.id().value(),
                        expectedSchemaVersion,
                        current.schemaVersion());
            }
            if (category.schemaVersion() != expectedSchemaVersion + 1) {
                throw new IllegalArgumentException("replacement category must increment schemaVersion by one");
            }
            if (!category.parentId().equals(current.parentId())) {
                throw new IllegalArgumentException("schema replacement cannot change the category parent");
            }
            categories.put(category.id(), category);
            return category;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(categories.get(categoryId));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Category> findAll() {
        lock.readLock().lock();
        try {
            return List.copyOf(new ArrayList<>(categories.values()));
        } finally {
            lock.readLock().unlock();
        }
    }
}
