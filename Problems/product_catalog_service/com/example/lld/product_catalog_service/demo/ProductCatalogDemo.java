package com.example.lld.product_catalog_service.demo;

import com.example.lld.product_catalog_service.exception.DuplicateSkuException;
import com.example.lld.product_catalog_service.exception.ProductValidationException;
import com.example.lld.product_catalog_service.exception.VersionConflictException;
import com.example.lld.product_catalog_service.model.AttributeDefinition;
import com.example.lld.product_catalog_service.model.AttributeKey;
import com.example.lld.product_catalog_service.model.AttributeScope;
import com.example.lld.product_catalog_service.model.AttributeType;
import com.example.lld.product_catalog_service.model.CategoryId;
import com.example.lld.product_catalog_service.model.ChangeCategorySchemaCommand;
import com.example.lld.product_catalog_service.model.CreateCategoryCommand;
import com.example.lld.product_catalog_service.model.CreateProductCommand;
import com.example.lld.product_catalog_service.model.EnumValue;
import com.example.lld.product_catalog_service.model.MediaAsset;
import com.example.lld.product_catalog_service.model.Product;
import com.example.lld.product_catalog_service.model.ProductBrowseQuery;
import com.example.lld.product_catalog_service.model.ProductChangeType;
import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ProductSchemaImpact;
import com.example.lld.product_catalog_service.model.ProductStatus;
import com.example.lld.product_catalog_service.model.SchemaChangeImpactReport;
import com.example.lld.product_catalog_service.model.Sku;
import com.example.lld.product_catalog_service.model.TextValue;
import com.example.lld.product_catalog_service.model.UpdateProductCommand;
import com.example.lld.product_catalog_service.model.Variant;
import com.example.lld.product_catalog_service.model.VariantId;
import com.example.lld.product_catalog_service.service.ProductCatalogService;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public final class ProductCatalogDemo {
    private static final CategoryId APPAREL = new CategoryId("apparel");
    private static final CategoryId SHIRTS = new CategoryId("shirts");
    private static final AttributeKey MATERIAL = new AttributeKey("material");
    private static final AttributeKey FIT = new AttributeKey("fit");
    private static final AttributeKey COLOR = new AttributeKey("color");
    private static final AttributeKey SIZE = new AttributeKey("size");
    private static final AttributeKey CARE = new AttributeKey("care_instructions");

    private ProductCatalogDemo() {
    }

    public static void main(String[] args) throws Exception {
        AtomicLong productSequence = new AtomicLong();
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T10:15:30Z"), ZoneOffset.UTC);
        ProductCatalogService catalog = ProductCatalogService.inMemory(
                () -> new ProductId("product-" + productSequence.incrementAndGet()),
                clock);

        List<AttributeDefinition> apparelSchema = apparelDefinitions(false);
        catalog.createCategory(new CreateCategoryCommand(
                APPAREL,
                "Apparel",
                Optional.empty(),
                apparelSchema,
                "taxonomy-admin"));
        catalog.createCategory(new CreateCategoryCommand(
                SHIRTS,
                "Shirts",
                Optional.of(APPAREL),
                List.of(enumDefinition(FIT, "Fit", AttributeScope.PRODUCT, "REGULAR", "SLIM")),
                "taxonomy-admin"));
        check(catalog.childrenOf(APPAREL).size() == 1, "category hierarchy should expose Shirts");
        check(catalog.effectiveSchemaFor(SHIRTS).productAttributes().containsKey(MATERIAL),
                "child category should inherit its parent schema");

        Sku redSmallSku = new Sku("SHIRT-OXFORD-RED-S");
        Product draft = catalog.createProduct(new CreateProductCommand(
                "Oxford Shirt",
                "A durable cotton Oxford shirt",
                "Northwind",
                SHIRTS,
                completeProductAttributes(),
                List.of(
                        variant("oxford-red-small", redSmallSku, "RED", "S"),
                        variant("oxford-blue-medium", new Sku("SHIRT-OXFORD-BLUE-M"), "BLUE", "M")),
                List.of(new MediaAsset(
                        URI.create("https://cdn.example.test/products/oxford-front.jpg"),
                        "Oxford shirt front view",
                        0)),
                "catalog-editor"));
        Product active = catalog.publishProduct(draft.id(), draft.version(), "catalog-publisher");
        check(active.status() == ProductStatus.ACTIVE, "complete draft should publish");
        check(catalog.getProduct(active.id()).equals(active), "get by ID should return the current snapshot");
        check(catalog.getProductBySku(redSmallSku).equals(active), "get by SKU should return its owner");

        List<Product> browseResults = catalog.browse(new ProductBrowseQuery(
                Optional.of(APPAREL),
                true,
                Optional.of("northwind"),
                Map.of(MATERIAL, new TextValue("cotton")),
                Map.of(COLOR, new EnumValue("RED")),
                Set.of(ProductStatus.ACTIVE)));
        check(browseResults.equals(List.of(active)), "structured browse should match inherited metadata");

        DuplicateSkuException duplicateSku = expectThrows(DuplicateSkuException.class, () ->
                catalog.createProduct(new CreateProductCommand(
                        "Another Shirt",
                        "Would collide with an existing sellable identifier",
                        "Contoso",
                        SHIRTS,
                        completeProductAttributes(),
                        List.of(variant("another-red-small", redSmallSku, "RED", "S")),
                        List.of(),
                        "catalog-editor")));

        Product incomplete = catalog.createProduct(new CreateProductCommand(
                "Incomplete Shirt",
                "A draft intentionally missing its inherited material attribute",
                "Northwind",
                SHIRTS,
                Map.of(FIT, new EnumValue("SLIM")),
                List.of(variant("incomplete-blue-small", new Sku("SHIRT-INCOMPLETE-BLUE-S"), "BLUE", "S")),
                List.of(),
                "catalog-editor"));
        ProductValidationException missingAttributes = expectThrows(
                ProductValidationException.class,
                () -> catalog.publishProduct(incomplete.id(), incomplete.version(), "catalog-publisher"));
        check(missingAttributes.issues().stream()
                        .anyMatch(issue -> issue.path().equals("attributes.material")),
                "publication failure should identify the missing inherited attribute");

        RaceResult race = runStaleUpdateRace(catalog, active);
        check(race.successes() == 1 && race.conflicts() == 1,
                "same-version concurrent updates should yield one success and one conflict");
        Product afterRace = catalog.getProduct(active.id());

        Product archived = catalog.archiveProduct(afterRace.id(), afterRace.version(), "catalog-admin");
        check(archived.status() == ProductStatus.ARCHIVED, "archive should change lifecycle state");
        check(catalog.getProductBySku(redSmallSku).equals(archived),
                "archived SKU should remain addressable and permanently owned");
        check(catalog.history(archived.id()).stream()
                        .anyMatch(version -> version.changeType() == ProductChangeType.ARCHIVED
                                && version.snapshot().status() == ProductStatus.ARCHIVED),
                "history should retain an immutable archived snapshot");

        Product reactivated = catalog.reactivateProduct(
                archived.id(),
                archived.version(),
                "catalog-publisher");
        check(reactivated.status() == ProductStatus.ACTIVE, "valid archived product should reactivate");

        SchemaChangeImpactReport impact = catalog.changeCategorySchema(new ChangeCategorySchemaCommand(
                APPAREL,
                1,
                apparelDefinitions(true),
                "taxonomy-admin"));
        Optional<ProductSchemaImpact> activeImpact = impact.affectedProducts().stream()
                .filter(candidate -> candidate.productId().equals(reactivated.id()))
                .findFirst();
        check(activeImpact.isPresent() && activeImpact.orElseThrow().newlyInvalid(),
                "adding a required inherited attribute should report the active product as newly invalid");
        check(activeImpact.orElseThrow().after().stream()
                        .anyMatch(issue -> issue.path().equals("attributes.care_instructions")),
                "schema impact should contain a structured path for the new requirement");
        Product schemaInvalidated = catalog.getProduct(reactivated.id());
        check(schemaInvalidated.status() == ProductStatus.DRAFT,
                "a newly invalid active product must be demoted to draft");
        check(schemaInvalidated.version() == reactivated.version() + 1,
                "schema invalidation must create an optimistic product revision");
        check(catalog.history(schemaInvalidated.id()).get(catalog.history(schemaInvalidated.id()).size() - 1)
                        .changeType() == ProductChangeType.SCHEMA_INVALIDATED,
                "schema-driven demotion should be visible in immutable audit history");
        check(catalog.browse(ProductBrowseQuery.activeCatalog()).stream()
                        .noneMatch(product -> product.id().equals(schemaInvalidated.id())),
                "schema-invalid products must not remain in the sellable catalog view");

        System.out.println("Product Catalog Service demo passed");
        System.out.println("  schema-invalidated product: " + schemaInvalidated.id()
                + " at version " + schemaInvalidated.version() + " (" + schemaInvalidated.status() + ")");
        System.out.println("  duplicate SKU rejected: " + duplicateSku.getMessage());
        System.out.println("  missing attribute rejected: " + missingAttributes.issues().get(0));
        System.out.println("  stale update race: " + race.successes() + " success, " + race.conflicts() + " conflict");
        System.out.println("  schema impacts reported: " + impact.affectedProducts().size());
    }

    private static RaceResult runStaleUpdateRace(ProductCatalogService catalog, Product base)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> runUpdate(
                    catalog, base, "description from editor A", "editor-a", ready, start));
            Future<String> second = executor.submit(() -> runUpdate(
                    catalog, base, "description from editor B", "editor-b", ready, start));
            ready.await();
            start.countDown();
            List<String> outcomes = List.of(first.get(), second.get());
            long successes = outcomes.stream().filter("success"::equals).count();
            long conflicts = outcomes.stream().filter("conflict"::equals).count();
            return new RaceResult(successes, conflicts);
        } finally {
            executor.shutdownNow();
        }
    }

    private static String runUpdate(
            ProductCatalogService catalog,
            Product base,
            String description,
            String actor,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            catalog.updateProduct(new UpdateProductCommand(
                    base.id(),
                    base.version(),
                    base.name(),
                    description,
                    base.brand(),
                    base.categoryId(),
                    base.attributes(),
                    base.variants(),
                    base.media(),
                    actor));
            return "success";
        } catch (VersionConflictException expected) {
            return "conflict";
        }
    }

    private static Map<AttributeKey, com.example.lld.product_catalog_service.model.AttributeValue>
            completeProductAttributes() {
        return Map.of(
                MATERIAL, new TextValue("cotton"),
                FIT, new EnumValue("REGULAR"));
    }

    private static Variant variant(String id, Sku sku, String color, String size) {
        return new Variant(
                new VariantId(id),
                sku,
                Map.of(
                        COLOR, new EnumValue(color),
                        SIZE, new EnumValue(size)));
    }

    private static List<AttributeDefinition> apparelDefinitions(boolean requireCareInstructions) {
        List<AttributeDefinition> base = List.of(
                new AttributeDefinition(
                        MATERIAL,
                        "Material",
                        AttributeType.TEXT,
                        AttributeScope.PRODUCT,
                        true,
                        Set.of()),
                enumDefinition(COLOR, "Colour", AttributeScope.VARIANT, "RED", "BLUE"),
                enumDefinition(SIZE, "Size", AttributeScope.VARIANT, "S", "M", "L"));
        if (!requireCareInstructions) {
            return base;
        }
        return List.of(
                base.get(0),
                base.get(1),
                base.get(2),
                new AttributeDefinition(
                        CARE,
                        "Care instructions",
                        AttributeType.TEXT,
                        AttributeScope.PRODUCT,
                        true,
                        Set.of()));
    }

    private static AttributeDefinition enumDefinition(
            AttributeKey key,
            String displayName,
            AttributeScope scope,
            String... allowedValues) {
        return new AttributeDefinition(
                key,
                displayName,
                AttributeType.ENUM,
                scope,
                true,
                Set.of(allowedValues));
    }

    private static <T extends Throwable> T expectThrows(Class<T> expectedType, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return expectedType.cast(actual);
            }
            throw new AssertionError(
                    "expected " + expectedType.getSimpleName() + " but caught " + actual,
                    actual);
        }
        throw new AssertionError("expected " + expectedType.getSimpleName() + " but nothing was thrown");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record RaceResult(long successes, long conflicts) {
    }
}
