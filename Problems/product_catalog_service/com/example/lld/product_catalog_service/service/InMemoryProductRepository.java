package com.example.lld.product_catalog_service.service;

import com.example.lld.product_catalog_service.exception.DuplicateSkuException;
import com.example.lld.product_catalog_service.exception.InvalidProductUpdateException;
import com.example.lld.product_catalog_service.exception.ProductAlreadyExistsException;
import com.example.lld.product_catalog_service.exception.ProductNotFoundException;
import com.example.lld.product_catalog_service.exception.VersionConflictException;
import com.example.lld.product_catalog_service.model.Product;
import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ProductVersion;
import com.example.lld.product_catalog_service.model.Sku;
import com.example.lld.product_catalog_service.model.Variant;
import com.example.lld.product_catalog_service.model.VariantId;
import com.example.lld.product_catalog_service.port.ProductRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InMemoryProductRepository implements ProductRepository {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Map<ProductId, Product> products = new HashMap<>();
    private final Map<Sku, ProductId> permanentSkuOwners = new HashMap<>();
    private final Map<ProductId, List<ProductVersion>> histories = new HashMap<>();

    @Override
    public Product insert(Product product, ProductVersion initialVersion) {
        lock.writeLock().lock();
        try {
            if (products.containsKey(product.id())) {
                throw new ProductAlreadyExistsException(product.id());
            }
            verifyHistoryEntry(product, initialVersion);
            ensureSkusAvailable(product);
            reserveSkus(product);
            products.put(product.id(), product);
            histories.put(product.id(), new ArrayList<>(List.of(initialVersion)));
            return product;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Product replace(Product product, long expectedVersion, ProductVersion newVersion) {
        lock.writeLock().lock();
        try {
            Product current = products.get(product.id());
            if (current == null) {
                throw new ProductNotFoundException(product.id());
            }
            if (current.version() != expectedVersion) {
                throw new VersionConflictException(
                        "product",
                        product.id().value(),
                        expectedVersion,
                        current.version());
            }
            if (product.version() != expectedVersion + 1) {
                throw new IllegalArgumentException("replacement product must increment version by one");
            }
            if (!product.createdAt().equals(current.createdAt())) {
                throw new InvalidProductUpdateException(product.id(), "createdAt is immutable");
            }
            verifyStableVariants(current, product);
            verifyHistoryEntry(product, newVersion);
            ensureSkusAvailable(product);
            reserveSkus(product);
            products.put(product.id(), product);
            histories.get(product.id()).add(newVersion);
            return product;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(products.get(productId));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        lock.readLock().lock();
        try {
            ProductId owner = permanentSkuOwners.get(sku);
            return owner == null ? Optional.empty() : Optional.ofNullable(products.get(owner));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Product> findAll() {
        lock.readLock().lock();
        try {
            return List.copyOf(new ArrayList<>(products.values()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ProductVersion> history(ProductId productId) {
        lock.readLock().lock();
        try {
            List<ProductVersion> history = histories.get(productId);
            if (history == null) {
                throw new ProductNotFoundException(productId);
            }
            return List.copyOf(history);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void ensureSkusAvailable(Product product) {
        Set<Sku> encountered = new HashSet<>();
        for (Variant variant : product.variants()) {
            if (!encountered.add(variant.sku())) {
                throw new DuplicateSkuException(variant.sku(), product.id());
            }
            ProductId owner = permanentSkuOwners.get(variant.sku());
            if (owner != null && !owner.equals(product.id())) {
                throw new DuplicateSkuException(variant.sku(), owner);
            }
        }
    }

    private void reserveSkus(Product product) {
        for (Variant variant : product.variants()) {
            permanentSkuOwners.put(variant.sku(), product.id());
        }
    }

    private static void verifyStableVariants(Product current, Product replacement) {
        Map<VariantId, Variant> replacementsById = replacement.variants().stream()
                .collect(Collectors.toMap(Variant::id, Function.identity()));
        for (Variant previous : current.variants()) {
            Variant next = replacementsById.get(previous.id());
            if (next == null) {
                throw new InvalidProductUpdateException(
                        current.id(),
                        "variant " + previous.id() + " cannot be removed");
            }
            if (!next.sku().equals(previous.sku())) {
                throw new InvalidProductUpdateException(
                        current.id(),
                        "SKU for variant " + previous.id() + " is immutable");
            }
        }
    }

    private static void verifyHistoryEntry(Product product, ProductVersion version) {
        if (!version.snapshot().equals(product)) {
            throw new IllegalArgumentException("history snapshot must equal the stored product");
        }
    }
}
