package com.example.lld.product_catalog_service.service;

import com.example.lld.product_catalog_service.exception.CategoryNotFoundException;
import com.example.lld.product_catalog_service.exception.InvalidProductStateException;
import com.example.lld.product_catalog_service.exception.ProductNotFoundException;
import com.example.lld.product_catalog_service.exception.ProductValidationException;
import com.example.lld.product_catalog_service.exception.SkuNotFoundException;
import com.example.lld.product_catalog_service.exception.VersionConflictException;
import com.example.lld.product_catalog_service.model.Category;
import com.example.lld.product_catalog_service.model.CategoryId;
import com.example.lld.product_catalog_service.model.CategorySchema;
import com.example.lld.product_catalog_service.model.ChangeCategorySchemaCommand;
import com.example.lld.product_catalog_service.model.CreateCategoryCommand;
import com.example.lld.product_catalog_service.model.CreateProductCommand;
import com.example.lld.product_catalog_service.model.Product;
import com.example.lld.product_catalog_service.model.ProductBrowseQuery;
import com.example.lld.product_catalog_service.model.ProductChangeType;
import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ProductSchemaImpact;
import com.example.lld.product_catalog_service.model.ProductStatus;
import com.example.lld.product_catalog_service.model.ProductVersion;
import com.example.lld.product_catalog_service.model.SchemaChangeImpactReport;
import com.example.lld.product_catalog_service.model.Sku;
import com.example.lld.product_catalog_service.model.UpdateProductCommand;
import com.example.lld.product_catalog_service.model.ValidationIssue;
import com.example.lld.product_catalog_service.port.CategoryRepository;
import com.example.lld.product_catalog_service.port.ProductIdGenerator;
import com.example.lld.product_catalog_service.port.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ProductCatalogService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductIdGenerator productIdGenerator;
    private final Clock clock;
    private final ProductValidator validator = new ProductValidator();
    private final ReentrantReadWriteLock catalogLock = new ReentrantReadWriteLock(true);

    public ProductCatalogService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductIdGenerator productIdGenerator,
            Clock clock) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
        this.productIdGenerator = Objects.requireNonNull(productIdGenerator, "productIdGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public static ProductCatalogService inMemory(ProductIdGenerator productIdGenerator, Clock clock) {
        return new ProductCatalogService(
                new InMemoryProductRepository(),
                new InMemoryCategoryRepository(),
                productIdGenerator,
                clock);
    }

    public Category createCategory(CreateCategoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        catalogLock.writeLock().lock();
        try {
            command.parentId().ifPresent(this::requireCategory);
            Instant now = clock.instant();
            Category category = new Category(
                    command.id(),
                    command.name(),
                    command.parentId(),
                    command.localDefinitions(),
                    1,
                    now,
                    command.actor());
            return categoryRepository.insert(category);
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    public Category getCategory(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        catalogLock.readLock().lock();
        try {
            return requireCategory(categoryId);
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public List<Category> childrenOf(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        catalogLock.readLock().lock();
        try {
            requireCategory(categoryId);
            return categoryRepository.findAll().stream()
                    .filter(category -> category.parentId().filter(categoryId::equals).isPresent())
                    .sorted(Comparator.comparing(Category::id))
                    .toList();
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public CategorySchema effectiveSchemaFor(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        catalogLock.readLock().lock();
        try {
            return effectiveSchema(categoryId);
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public Product createProduct(CreateProductCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        catalogLock.writeLock().lock();
        try {
            requireCategory(command.categoryId());
            ProductId productId = Objects.requireNonNull(
                    productIdGenerator.nextProductId(),
                    "productIdGenerator returned null");
            Instant now = clock.instant();
            Product product = new Product(
                    productId,
                    command.name(),
                    command.description(),
                    command.brand(),
                    command.categoryId(),
                    command.attributes(),
                    command.variants(),
                    command.media(),
                    ProductStatus.DRAFT,
                    1,
                    now,
                    now,
                    command.actor());
            validate(product, false);
            ProductVersion version = historyEntry(product, ProductChangeType.CREATED, command.actor(), now);
            return productRepository.insert(product, version);
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    public Product updateProduct(UpdateProductCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        catalogLock.writeLock().lock();
        try {
            Product current = requireProduct(command.productId());
            requireExpectedVersion(current, command.expectedVersion());
            if (current.status() == ProductStatus.ARCHIVED) {
                throw new InvalidProductStateException(current.id(), "updated", current.status());
            }
            requireCategory(command.categoryId());
            Instant now = clock.instant();
            Product revised = current.revise(
                    command.name(),
                    command.description(),
                    command.brand(),
                    command.categoryId(),
                    command.attributes(),
                    command.variants(),
                    command.media(),
                    now,
                    command.actor());
            validate(revised, revised.status() == ProductStatus.ACTIVE);
            return productRepository.replace(
                    revised,
                    command.expectedVersion(),
                    historyEntry(revised, ProductChangeType.UPDATED, command.actor(), now));
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    public Product publishProduct(ProductId productId, long expectedVersion, String actor) {
        return transition(
                productId,
                expectedVersion,
                actor,
                ProductStatus.DRAFT,
                ProductStatus.ACTIVE,
                ProductChangeType.PUBLISHED,
                true);
    }

    public Product archiveProduct(ProductId productId, long expectedVersion, String actor) {
        Objects.requireNonNull(productId, "productId must not be null");
        catalogLock.writeLock().lock();
        try {
            Product current = requireProduct(productId);
            requireExpectedVersion(current, expectedVersion);
            if (current.status() == ProductStatus.ARCHIVED) {
                throw new InvalidProductStateException(
                        productId,
                        ProductStatus.ARCHIVED,
                        ProductStatus.ARCHIVED);
            }
            return saveTransition(current, expectedVersion, actor, ProductStatus.ARCHIVED,
                    ProductChangeType.ARCHIVED, false);
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    public Product reactivateProduct(ProductId productId, long expectedVersion, String actor) {
        return transition(
                productId,
                expectedVersion,
                actor,
                ProductStatus.ARCHIVED,
                ProductStatus.ACTIVE,
                ProductChangeType.REACTIVATED,
                true);
    }

    public Product getProduct(ProductId productId) {
        Objects.requireNonNull(productId, "productId must not be null");
        catalogLock.readLock().lock();
        try {
            return requireProduct(productId);
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public Product getProductBySku(Sku sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        catalogLock.readLock().lock();
        try {
            return productRepository.findBySku(sku).orElseThrow(() -> new SkuNotFoundException(sku));
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public List<ProductVersion> history(ProductId productId) {
        Objects.requireNonNull(productId, "productId must not be null");
        catalogLock.readLock().lock();
        try {
            requireProduct(productId);
            return productRepository.history(productId);
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public List<Product> browse(ProductBrowseQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        catalogLock.readLock().lock();
        try {
            Set<CategoryId> categoryIds = query.categoryId()
                    .map(id -> query.includeDescendantCategories()
                            ? descendantCategoryIds(id)
                            : Set.of(requireCategory(id).id()))
                    .orElseGet(Set::of);

            return productRepository.findAll().stream()
                    .filter(product -> query.statuses().isEmpty() || query.statuses().contains(product.status()))
                    .filter(product -> categoryIds.isEmpty() || categoryIds.contains(product.categoryId()))
                    .filter(product -> query.brand()
                            .map(brand -> brand.equalsIgnoreCase(product.brand()))
                            .orElse(true))
                    .filter(product -> containsAll(product.attributes(), query.productAttributes()))
                    .filter(product -> query.variantAttributes().isEmpty()
                            || product.variants().stream().anyMatch(variant ->
                                    containsAll(variant.attributes(), query.variantAttributes())))
                    .sorted(Comparator.comparing(Product::id))
                    .toList();
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    public SchemaChangeImpactReport changeCategorySchema(ChangeCategorySchemaCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        catalogLock.writeLock().lock();
        try {
            Category current = requireCategory(command.categoryId());
            if (current.schemaVersion() != command.expectedSchemaVersion()) {
                throw new VersionConflictException(
                        "category schema",
                        current.id().value(),
                        command.expectedSchemaVersion(),
                        current.schemaVersion());
            }

            Set<CategoryId> dependentCategoryIds = descendantCategoryIds(current.id());
            List<Product> dependentProducts = productRepository.findAll().stream()
                    .filter(product -> dependentCategoryIds.contains(product.categoryId()))
                    .sorted(Comparator.comparing(Product::id))
                    .toList();
            Map<ProductId, List<ValidationIssue>> before = new HashMap<>();
            for (Product product : dependentProducts) {
                before.put(product.id(), validator.validate(product, effectiveSchema(product.categoryId()), true));
            }

            Instant changedAt = clock.instant();
            Category changed = current.changeSchema(command.localDefinitions(), changedAt, command.actor());
            categoryRepository.replace(changed, command.expectedSchemaVersion());

            List<ProductSchemaImpact> impacts = new ArrayList<>();
            for (Product product : dependentProducts) {
                List<ValidationIssue> after = validator.validate(
                        product,
                        effectiveSchema(product.categoryId()),
                        true);
                List<ValidationIssue> previous = before.get(product.id());
                boolean demoteFromActive = product.status() == ProductStatus.ACTIVE && !after.isEmpty();
                if (!sameIssues(previous, after) || demoteFromActive) {
                    impacts.add(new ProductSchemaImpact(
                            product.id(),
                            product.version(),
                            product.status(),
                            previous,
                            after));
                }
                if (demoteFromActive) {
                    Product demoted = product.transitionTo(
                            ProductStatus.DRAFT,
                            changedAt,
                            command.actor());
                    productRepository.replace(
                            demoted,
                            product.version(),
                            historyEntry(
                                    demoted,
                                    ProductChangeType.SCHEMA_INVALIDATED,
                                    command.actor(),
                                    changedAt));
                }
            }
            return new SchemaChangeImpactReport(
                    current.id(),
                    current.schemaVersion(),
                    changed.schemaVersion(),
                    changedAt,
                    impacts);
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    private Product transition(
            ProductId productId,
            long expectedVersion,
            String actor,
            ProductStatus requiredCurrent,
            ProductStatus target,
            ProductChangeType changeType,
            boolean validateForPublication) {
        Objects.requireNonNull(productId, "productId must not be null");
        catalogLock.writeLock().lock();
        try {
            Product current = requireProduct(productId);
            requireExpectedVersion(current, expectedVersion);
            if (current.status() != requiredCurrent) {
                throw new InvalidProductStateException(productId, current.status(), target);
            }
            return saveTransition(
                    current,
                    expectedVersion,
                    actor,
                    target,
                    changeType,
                    validateForPublication);
        } finally {
            catalogLock.writeLock().unlock();
        }
    }

    private Product saveTransition(
            Product current,
            long expectedVersion,
            String actor,
            ProductStatus target,
            ProductChangeType changeType,
            boolean validateForPublication) {
        String checkedActor = requireActor(actor);
        Instant now = clock.instant();
        Product changed = current.transitionTo(target, now, checkedActor);
        if (validateForPublication) {
            validate(changed, true);
        }
        return productRepository.replace(
                changed,
                expectedVersion,
                historyEntry(changed, changeType, checkedActor, now));
    }

    private void validate(Product product, boolean requirePublicationCompleteness) {
        List<ValidationIssue> issues = validator.validate(
                product,
                effectiveSchema(product.categoryId()),
                requirePublicationCompleteness);
        if (!issues.isEmpty()) {
            throw new ProductValidationException(product.id(), issues);
        }
    }

    private CategorySchema effectiveSchema(CategoryId categoryId) {
        List<Category> lineage = new ArrayList<>();
        Set<CategoryId> visited = new HashSet<>();
        Category cursor = requireCategory(categoryId);
        while (true) {
            if (!visited.add(cursor.id())) {
                throw new IllegalStateException("category hierarchy contains a cycle at " + cursor.id());
            }
            lineage.add(cursor);
            if (cursor.parentId().isEmpty()) {
                break;
            }
            cursor = requireCategory(cursor.parentId().orElseThrow());
        }
        Collections.reverse(lineage);
        CategorySchema schema = CategorySchema.empty();
        for (Category category : lineage) {
            schema = schema.overlay(category.localDefinitions());
        }
        return schema;
    }

    private Set<CategoryId> descendantCategoryIds(CategoryId rootId) {
        requireCategory(rootId);
        Set<CategoryId> result = new HashSet<>();
        result.add(rootId);
        List<Category> categories = categoryRepository.findAll();
        boolean expanded;
        do {
            expanded = false;
            for (Category category : categories) {
                if (category.parentId().filter(result::contains).isPresent()
                        && result.add(category.id())) {
                    expanded = true;
                }
            }
        } while (expanded);
        return Set.copyOf(result);
    }

    private Category requireCategory(CategoryId categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private Product requireProduct(ProductId productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static void requireExpectedVersion(Product product, long expectedVersion) {
        if (product.version() != expectedVersion) {
            throw new VersionConflictException(
                    "product",
                    product.id().value(),
                    expectedVersion,
                    product.version());
        }
    }

    private static ProductVersion historyEntry(
            Product product,
            ProductChangeType changeType,
            String actor,
            Instant recordedAt) {
        return new ProductVersion(product.version(), changeType, actor, recordedAt, product);
    }

    private static boolean containsAll(Map<?, ?> actual, Map<?, ?> expected) {
        return expected.entrySet().stream()
                .allMatch(entry -> Objects.equals(actual.get(entry.getKey()), entry.getValue()));
    }

    private static boolean sameIssues(
            List<ValidationIssue> first,
            List<ValidationIssue> second) {
        Comparator<ValidationIssue> byPathAndMessage = Comparator
                .comparing(ValidationIssue::path)
                .thenComparing(ValidationIssue::message);
        return first.stream().sorted(byPathAndMessage).toList()
                .equals(second.stream().sorted(byPathAndMessage).toList());
    }

    private static String requireActor(String actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        String trimmed = actor.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        return trimmed;
    }
}
