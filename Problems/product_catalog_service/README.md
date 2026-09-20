# Product Catalog Service

## Problem Description

Design the authoritative source for product identity, classification, descriptive
metadata, variants, and publication lifecycle. Products must support different
category-specific attributes without adding product-type conditionals throughout the
codebase.

This service owns product data. Product Search owns discovery and relevance;
Inventory owns quantities; Promotion owns discounts; and Order Management owns
purchase snapshots.

## 60-Minute Senior/Staff Interview Guide

### Candidate-facing question

> Design the authoritative Product Catalog Service for a multi-category commerce
> platform. Products have stable identities, category-specific typed attributes,
> sellable variants/SKUs, media, lifecycle, and history. Support safe concurrent edits
> and category-schema evolution while keeping price, stock, search, and promotions in
> their proper domains.

The expected LLD is the write-side source of truth. A search index or generic JSON
document store may be an output, but neither replaces aggregate invariants.

### Suggested interview plan

| Time | Expected output |
| ---: | --- |
| 0–5 min | Clarify ownership, product/variant distinction, schema, lifecycle, and history |
| 5–12 min | Agree on create/edit/publish/archive and explicitly exclude price, stock, and ranking |
| 12–23 min | Model product aggregate, variant, SKU, category schema, typed values, and versions |
| 23–33 min | Define commands, reads, validation results, and optimistic concurrency |
| 33–43 min | Walk publication, duplicate SKU, and two-editor races |
| 43–52 min | Handle schema changes, projections/events, bulk operations, and compatibility |
| 52–58 min | Defend SOLID/pattern choices and alternatives |
| 58–60 min | Summarize invariants and tests |

### Clarifying questions and strong assumptions

| Ask | Strong working assumption when unspecified |
| --- | --- |
| What does catalog own? | Identity, taxonomy, descriptive metadata, variants, media references, lifecycle, and history—not price, stock, or search relevance |
| Product versus variant? | Product owns shared meaning; each sellable variant has stable variant ID, unique SKU, and variant-scoped attributes |
| Are category fields fixed? | No; administrators define versioned typed attribute schemas with required/product/variant scope |
| Can an active product be edited? | Yes through an expected-version command; the resulting active snapshot must remain valid atomically |
| Can a SKU be reused after archive? | No; historical orders and integrations require permanent ownership |
| What does a schema change do? | Validate affected descendants, report impact, and apply an explicit demotion/migration policy |
| How are reads served? | ID/SKU reads may use the source store; browse/search consumes an eventually consistent projection |
| Is bulk import in scope? | Not for the first slice; the same command validator returns per-row errors in a later batch coordinator |

**In scope:** category hierarchy, typed schemas, product/variant commands, validation,
publication/archive, permanent SKU uniqueness, optimistic versions, history, and
schema-impact analysis.

**Out of scope:** inventory quantities, dynamic prices, discount calculation, search
ranking, image bytes, checkout, and order history. Catalog exposes references and
immutable snapshots to those owners.

### Expected solution and design-principle reasoning

#### Ownership model

| Type | Owned decision |
| --- | --- |
| `Product` | Aggregate lifecycle, shared attributes, variants, media references, and version |
| `Variant` | Stable sellable identity, SKU, and variant-scoped attribute values |
| `Sku`, `ProductId`, `VariantId`, `AttributeKey` | Normalized immutable identity/value semantics |
| `Category` / `CategorySchema` | Taxonomy placement and effective versioned attribute definitions |
| `AttributeDefinition` | Type, scope, required flag, enum domain, and validation constraints |
| `AttributeValue` | Typed value hierarchy; raw maps never reach the aggregate unchecked |
| `ProductValidator` | Cross-field and effective-schema validation policy |
| `ProductCatalogService` | Command orchestration across category/product repositories and audit/events |
| `ProductRepository` | Atomic expected-version update plus permanent unique-SKU constraint |

The aggregate protects one product's state; the repository protects uniqueness across
products. SKU uniqueness cannot live only in `Product` because no product can see all
other owners.

#### SOLID and justified patterns

| Choice | Reasoning |
| --- | --- |
| **SRP** | Product owns lifecycle, schema owns definitions, validator owns validation, repository owns cross-product uniqueness, and search remains outside |
| **OCP** | Category definitions make new attributes/data shapes configuration-driven; typed values extend validation without product-type conditionals |
| **LSP** | Every `AttributeValue` provides stable type/equality/display semantics; validators do not downcast to surprise behavior |
| **ISP** | Category and product repositories, ID generation, event publication, and media references are narrow independent ports |
| **DIP** | The application service depends on repositories, clock, and ID ports so commands and stale-write tests run in memory |
| **Repository + Unit of Work** | Persistence details stay outside the aggregate while version, SKU index, product write, audit, and outbox share one transaction |
| **Specification** | Effective-schema and browse predicates are composable named rules; do not hard-code `if category == ...` branches |
| **Factory** | A value factory can parse external input into validated typed `AttributeValue`s before aggregate construction |

State pattern objects are unnecessary when guarded aggregate methods make the small
`DRAFT → ACTIVE → ARCHIVED` lifecycle clearer. Strategy is appropriate only for a
real policy variation such as publication approval or schema-migration policy.

#### Interview API contract

```java
Product create(CreateProductCommand command, String idempotencyKey);
Product update(ProductId id, long expectedVersion, UpdateProductCommand command);
Product publish(ProductId id, long expectedVersion, Actor actor);
Product archive(ProductId id, long expectedVersion, Actor actor);
Optional<Product> findBySku(Sku sku);
SchemaChangeImpactReport changeCategorySchema(
    CategoryId id, long expectedVersion, ChangeCategorySchemaCommand command);
```

Commands return immutable snapshots or typed validation/version/uniqueness errors.
The expected version belongs in every mutation contract; HTTP transport can map it to
an ETag/`If-Match` without leaking HTTP into the domain.

#### Invariants and transaction boundary

1. Product, variant, and SKU identities are stable; an existing variant cannot be
   silently removed or re-keyed if downstream history refers to it.
2. One SKU has one permanent owner, including after archive.
3. An `ACTIVE` product satisfies the effective category schema and has at least one
   valid sellable variant.
4. Product- and variant-scoped attributes are known, correctly typed, and unique by
   scoped key; variant combinations are structurally unique.
5. One stale writer fails—no last-write-wins overwrite of a newer version.
6. A category-schema commit and its impact policy are atomic with affected product
   revisions in this reference implementation.
7. Every successful mutation appends an immutable audit/version record; events are
   written through an outbox in a production transaction.

### Concrete walkthrough: schema change invalidates active products

An administrator adds required variant attribute `material` to a parent category.
The service resolves descendants and validates their current products against the new
effective schema without mutating them. It returns a structured impact report. On
commit, the reference policy appends an audited `SCHEMA_INVALIDATED` version and
moves newly invalid active products to `DRAFT`, so the active view never contains
invalid data. A Staff-level alternative is a staged schema version with a migration
deadline and last-known-good published projection to avoid abrupt assortment loss.

### Questions an interviewer will ask

| Follow-up question | Expected strong answer |
| --- | --- |
| Why not `Map<String, Object>`? | It pushes type/scope validation into every caller, permits invalid states, and makes schema evolution/audit ambiguous |
| Where is SKU uniqueness enforced? | In an atomic database unique constraint/index plus repository transaction; an in-memory pre-check alone races |
| Why separate search? | Catalog is the consistent write model; search ranking and denormalized filters evolve independently and consume versioned events |
| How do two editors update? | Both send expected version; the repository compare-and-set lets one commit and returns an explicit conflict to the other |
| Can archived SKUs be reused? | No; retain permanent ownership so historical orders, returns, and integrations keep their meaning |
| How are category attributes inherited? | Resolve root-to-leaf definitions with explicit override rules and store the schema/version used for validation |
| What if event publication fails? | Product change, audit, and outbox record commit together; an asynchronous publisher retries with stable event IDs |
| How does bulk import work? | Parse/validate rows independently, use command contracts, report per-row errors, and define batch atomicity rather than bypassing invariants |
| How do prices and stock appear in a product page? | A composition/query layer joins catalog identity with price and availability projections; those values are not mutated in Product |

### Senior and Staff expectations

- A **Senior** answer clearly separates product and variant, uses typed value objects,
  enforces lifecycle and permanent SKU uniqueness, handles stale writers, and tests
  publication plus schema validation.
- A **Staff** answer additionally defines taxonomy/schema ownership, staged migration,
  source-of-truth versus search projections, audit/retention, bulk-import semantics,
  event compatibility, and rollout behavior when a definition changes widely.

### Red flags and scoring focus

- Red flags: category subclasses for every product type, mutable attribute maps,
  catalog owning stock/price/search, reusing archived SKUs, service-only uniqueness
  checks, last-write-wins edits, and schema changes that silently drop data.
- Score highly for: explicit ownership boundaries, aggregate versus repository
  invariants, typed schemas, optimistic concurrency, a safe schema-evolution story,
  principled patterns, and deterministic duplicate/stale-writer tests.

### Interview variations and expected solutions

The source collections ask “catalog” at several different boundaries. Treat these as
distinct questions and reuse the canonical types deliberately; do not combine catalog,
inventory, pricing, search, cart, and order behavior into one aggregate.

#### Variation 1 — Guitar inventory and typed specification search

**Question.** Store guitars with serial number, price, builder, model, type, back wood,
and top wood; let a customer search by a partially specified guitar.

**Expected Senior solution.** Model `Guitar` identity separately from immutable
`GuitarSpec`; make search a query/specification that matches only supplied fields;
normalize enums/value objects and keep money decimal/minor-unit based. For a small
in-memory exercise, one `Inventory` repository plus `addGuitar` and `search` is enough—
category schemas, publication workflow, and events would be speculative.

**Staff extension.** Explain how the fixed `GuitarSpec` evolves into the canonical
typed `AttributeDefinition`/`AttributeValue` model when categories become dynamic,
and how an indexed read projection replaces an in-memory scan.

**Canonical code fit.** Reuse the value, schema, repository, and browse concepts from
either language; omit variants and publication lifecycle for this deliberately small
variation.

This variation incorporates the primer's requirements-only [Guitar Inventory prompt](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L76).

#### Variation 2 — Catalog embedded in an online shopping system

**Question.** Model a browseable product catalog as part of shopping, while carts,
orders, payments, and shipment also exist.

**Expected Senior solution.** Keep `Product`/`Category`/`Sku` catalog ownership
separate from `CartLine`, `OrderLine` snapshots, payment, and stock. Expose catalog
queries and immutable product snapshots; checkout copies purchase facts instead of
holding a mutable product reference. The canonical product/variant/SKU model applies,
while order/cart collaborators remain separate aggregates.

**Staff extension.** Define event-driven projections and compatibility when catalog
data changes after an order, plus composition of price and availability on product
pages. Preserve bounded contexts even if the interview implementation is one process.

**Canonical code fit.** The Java/Python product, variant, SKU, repository, and
lifecycle types are the catalog component; compose them with the separate order,
inventory, promotion, and payment solutions rather than merging aggregates.

The GPL [Online Shopping Service solution](../../References/awesome-low-level-design/solutions/java/src/onlineshoppingservice/) ([pinned source](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb/solutions/java/src/onlineshoppingservice)) supplies the broader commerce variation. Its Java, C++, C#, Go, and Python ports represent one solution family rather than five variations.

#### Variation 3 — Amazon-style catalog, search, and reviews

**Question.** Design products, categories, search, reviews/ratings, and seller-facing
catalog operations for a marketplace.

**Expected Senior solution.** Use the canonical catalog aggregate for authoritative
identity and metadata, but make `CatalogSearch` a read port/projection and make
`Review` a user-owned aggregate keyed by product. Seller listing/offer identity is
separate when several sellers can sell one product. Do not add search/review mutation
to `ProductCatalogService` merely because the API composes them.

**Staff extension.** Define moderation, aggregate-rating projection, seller isolation,
schema/taxonomy governance, index lag, and event compatibility. Clarify whether a
marketplace listing or global product is the sellable SKU owner.

**Canonical code fit.** Use the complete catalog write model unchanged, then add
separate `ReviewRepository`, seller-offer aggregate, and read projection ports; neither
Java nor Python product code needs review/search branches.

Kumar's unlicensed [`amazon_new` implementation](../../References/kumaransg-LLD/Low_level_Design_Problems/amazon_new/) ([pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/amazon_new)) is merged here as a compact domain sketch; the canonical implementation supplies the missing typed validation, concurrency, and lifecycle guarantees.

#### Variation 4 — Catalog/Search interface split

**Question.** Provide `searchProductsByName` and `searchProductsByCategory` through a
catalog/search abstraction while the wider Amazon model contains accounts, carts,
orders, and notifications.

**Expected Senior solution.** Apply ISP: a read-only search interface should not expose
catalog administration, and the command service should not depend on search-index
implementation details. Back small data with a repository scan; preserve query
contracts so a projection can replace it. Reuse canonical IDs, category, product, and
variant snapshots; omit schema administration if the prompt fixes the fields.

**Staff extension.** Separate source-of-truth writes from eventually consistent
search, version projection events, define pagination/filter stability, and describe
reindex/dual-read migration.

**Canonical code fit.** Reuse immutable product/category snapshots and repository
ports. The provided in-memory browse is the interview implementation of the read
contract; production substitutes an index adapter.

Kumar's distinct unlicensed [`oops/amazon` variation](../../References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/) ([pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon)) motivates this interface-focused version; it is not a duplicate of `amazon_new`.

#### Variation 5 — Product catalog coupled to stock in order booking

**Question.** Add categories and products, update stock, list inventory, and place an
order for a requested quantity.

**Expected Senior solution.** Replace `Product extends Category` with composition:
`Product` references `CategoryId`. Keep descriptive catalog data in the canonical
catalog and place quantity/reservation under an inventory aggregate keyed by SKU.
Order placement requests a reservation and stores a product snapshot; it does not
decrement a mutable field on the catalog product.

**Staff extension.** Define reservation expiry/idempotency, last-unit concurrency,
outbox events, and service ownership. If implemented in one process, preserve the
ports and transaction boundary so catalog and inventory can evolve independently.

**Canonical code fit.** Combine the catalog's stable SKU/snapshots with the separate
Inventory Reservation and Order Management ports. Do not add quantity mutation to
either catalog implementation.

The unlicensed [Order-Booking catalog fragment](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/src/com/flipkart/catalog/) ([pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/src/com/flipkart/catalog)) provides this machine-coding variation; its inheritance and stock coupling are review inputs corrected by the expected solution above.

## Functional Requirements

1. Create and update products with stable product IDs and unique SKU variants.
2. Model brand, category hierarchy, attributes, variants, and media references.
3. Define typed attribute schemas and required attributes per category.
4. Validate product and variant data against the selected category schema.
5. Support `DRAFT`, `ACTIVE`, and `ARCHIVED` lifecycle states.
6. Publish only complete, valid products to the sellable catalog view.
7. Retrieve products by product ID or SKU and browse/filter structured metadata.
8. Preserve version and audit history and reject stale concurrent updates.
9. Keep archived product snapshots addressable for historical orders.

## Suggested Domain Model

| Type | Responsibility |
| --- | --- |
| `Product` | Aggregate root for shared product data and lifecycle |
| `Variant` | Sellable variation with its own SKU and attributes |
| `Sku` | Unique stock-keeping identifier value object |
| `Category` | Node in the product taxonomy |
| `AttributeDefinition` | Name, type, constraints, and applicability |
| `AttributeValue` | Validated product or variant attribute |
| `MediaAsset` | Reference and presentation metadata for product media |
| `ProductVersion` | Version, actor, timestamp, and change history |

## Business Rules and State Transitions

- Product IDs and SKUs are stable; an archived SKU cannot silently be reused for a
  different product.
- A product may become `ACTIVE` only when all required data and variant attributes
  are valid.
- Variant combinations and SKUs must be unique within their relevant scopes.
- A category cannot be removed while products or child categories still depend on it.
- Editing uses optimistic version checks so stale writes cannot overwrite newer data.
- Archiving removes a product from the active catalog but preserves historical reads.
- Product price calculation, stock availability, and promotion evaluation remain
  outside this aggregate.

## Concurrency and Failure Handling

- Concurrent updates using the same old version must produce one success and one
  explicit version conflict.
- SKU uniqueness must hold under concurrent product creation.
- A failed publication must not expose a partially updated product.
- Category-schema changes must identify products that become invalid rather than
  silently discarding data.

## Demonstration Scenarios

1. Create and activate a product with colour and size variants.
2. Reject a duplicate SKU.
3. Reject publication when a required category attribute is missing.
4. Demonstrate a stale concurrent update conflict.
5. Archive a product while retaining its historical snapshot.
6. Change a category schema and report affected products.

## Extensions

- Localised names and descriptions
- Bundles and kits
- Bulk import with per-row validation
- Merchandising collections and approvals

## Related Problems

- [Product Search System](../product_search_system/README.md)
- [Inventory Reservation Service](../inventory_reservation_service/README.md)
- [Coupon / Promotion Engine](../coupon_promotion_engine/README.md)
- [Order Management System](../order_processing_system/README.md)

---

## Java 17 Reference Solution

### Implementation Status

| Requirement | Status | Implementation |
| --- | --- | --- |
| Category hierarchy and typed schemas | Complete | Parent/child `Category` snapshots, inherited `CategorySchema`, scoped `AttributeDefinition`, and sealed typed values |
| Stable product and variant identity | Complete | Generated `ProductId`; immutable `VariantId` to `Sku` association; existing variants cannot be removed or re-keyed |
| Atomic, permanent SKU uniqueness | Complete | `InMemoryProductRepository` validates and reserves all SKUs under one write lock; archive never releases ownership |
| Draft, active, and archived lifecycle | Complete | Explicit publish, archive, validated reactivation, and audited schema-invalidation demotion with meaningful state errors |
| Validation and publication | Complete | Drafts permit missing required fields; publication and active edits require the full effective category schema |
| Immutable snapshots and audit history | Complete | Defensive-copy records plus a `ProductVersion` snapshot for every successful mutation |
| Optimistic concurrency | Complete | Expected versions are checked by both the service and repository; stale writes raise `VersionConflictException` |
| ID/SKU reads and structured browse | Complete | Exact lookups plus category descendants, brand, status, product attributes, and same-variant attribute filters |
| Category schema impact analysis | Complete | A schema commit atomically reports impact and audits/demotes newly invalid active products to `DRAFT` |
| Deterministic time and runnable scenarios | Complete | Injected `Clock`; demo covers success, duplicate SKU, missing attributes, stale writers, archive/reactivate, and schema impact |

### Architecture and Design Choices

- `Product` and `Category` are immutable snapshots. Maps and lists are copied on
  construction, so reads and `ProductVersion` history cannot be changed by callers.
- `AttributeValue` is a sealed hierarchy (`TextValue`, `IntegerValue`,
  `DecimalValue`, `BooleanValue`, and `EnumValue`). `AttributeDefinition` declares
  its type, product/variant scope, required flag, and enum domain without product
  type conditionals.
- Category definitions are local to a taxonomy node. The service walks root to leaf
  and overlays definitions to produce an effective inherited schema. A child may
  deliberately refine a definition with the same scoped key.
- A draft must be structurally sound: supplied values must be known and correctly
  typed, and variant IDs, SKUs, and attribute combinations must be unique. Missing
  required attributes and an empty variant list remain legal until publication.
- Every write carries an expected version and creates a new aggregate plus an audit
  snapshot. The repository repeats the version check inside its write lock, so the
  optimistic-concurrency guarantee does not depend only on an earlier service read.
- SKU validation and reservation happen together under the repository write lock.
  The ownership index is permanent, including after archive. Updates preserve every
  existing variant ID and its SKU while still allowing new variants to be added.
- `ProductCatalogService` uses a fair catalog lock to keep cross-repository actions
  (schema reads plus product writes) atomic in this in-memory reference. Repositories
  remain explicit ports so a production implementation can replace the lock with
  database transactions, unique constraints, and compare-and-set updates.
- Schema changes do not silently discard products. The returned
  `SchemaChangeImpactReport` shows structured before/after validation issues for
  every affected product in the changed category subtree. Under the catalog write
  lock, the schema commit atomically gives each newly invalid active product an
  audited `SCHEMA_INVALIDATED` revision and moves it to `DRAFT`, preserving the
  invariant that the active browse view contains only sellable data.
- Archiving changes catalog visibility only when callers filter for `ACTIVE`; the
  current archived product, its SKU lookup, and every prior snapshot remain
  addressable for historical order use.

### Source Structure

```text
com/example/lld/product_catalog_service/
├── model/
│   ├── AttributeDefinition.java
│   ├── AttributeKey.java
│   ├── AttributeScope.java
│   ├── AttributeType.java
│   ├── AttributeValue.java
│   ├── BooleanValue.java
│   ├── DecimalValue.java
│   ├── EnumValue.java
│   ├── IntegerValue.java
│   ├── TextValue.java
│   ├── ModelSupport.java
│   ├── Category.java
│   ├── CategoryId.java
│   ├── CategorySchema.java
│   ├── Product.java
│   ├── ProductId.java
│   ├── ProductStatus.java
│   ├── ProductChangeType.java
│   ├── ProductVersion.java
│   ├── Variant.java
│   ├── VariantId.java
│   ├── Sku.java
│   ├── MediaAsset.java
│   ├── CreateCategoryCommand.java
│   ├── ChangeCategorySchemaCommand.java
│   ├── CreateProductCommand.java
│   ├── UpdateProductCommand.java
│   ├── ProductBrowseQuery.java
│   ├── ValidationIssue.java
│   ├── ProductSchemaImpact.java
│   └── SchemaChangeImpactReport.java
├── port/
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   └── ProductIdGenerator.java
├── service/
│   ├── ProductCatalogService.java
│   ├── ProductValidator.java
│   ├── InMemoryCategoryRepository.java
│   └── InMemoryProductRepository.java
├── exception/
│   ├── CatalogException.java
│   ├── CategoryAlreadyExistsException.java
│   ├── CategoryNotFoundException.java
│   ├── DuplicateSkuException.java
│   ├── InvalidProductStateException.java
│   ├── InvalidProductUpdateException.java
│   ├── ProductAlreadyExistsException.java
│   ├── ProductNotFoundException.java
│   ├── ProductValidationException.java
│   ├── SkuNotFoundException.java
│   └── VersionConflictException.java
└── demo/
    └── ProductCatalogDemo.java
```

`ModelSupport.java` is package-private; it centralises defensive copies and input
checks for the public model records.

### Compile and Run

Run from the repository root. Compilation output is written to a disposable folder
under `/private/tmp`, so the problem directory remains source-only.

```bash
CATALOG_OUT="$(mktemp -d /private/tmp/product-catalog.XXXXXX)"
find Problems/product_catalog_service/com -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -d "$CATALOG_OUT"
java -cp "$CATALOG_OUT" \
  com.example.lld.product_catalog_service.demo.ProductCatalogDemo
```

Expected summary:

```text
Product Catalog Service demo passed
  stale update race: 1 success, 1 conflict
```

## Python 3 Reference Solution

The Python 3.9+ answer is a complete idiomatic implementation of the same write-side
catalog boundary. Frozen dataclasses and defensive mapping proxies preserve
snapshots; `AttributeDefinition` performs strict typed validation; the repository
atomically enforces permanent SKU ownership and expected versions; and the service
owns category inheritance, lifecycle, schema impact, and audited demotion.

### One-hour Python coding scope

- **Implement live:** `Product`, `Variant`, `AttributeDefinition`, lifecycle enum,
  validator, repository expected-version/SKU checks, `create`/`publish`/`update`, and
  duplicate-SKU plus stale-editor tests.
- **Add if time remains:** inherited schemas and the schema-invalidates-active-product
  transition; the provided module includes them as the Staff follow-up implementation.
- **Explain, do not type:** create-command idempotency receipts, HTTP/ETags, ORM
  mappings, database unique constraints, outbox/search projection, bulk import,
  schema migration jobs, and observability.

```text
python/
├── solution.py       # catalog model, repository, validation, lifecycle, schema changes
└── test_solution.py  # publication, duplicate SKU, stale race, schema invalidation
```

Run from this problem directory:

```bash
cd python
python3 test_solution.py
```

The tests use only the standard library and include strict typed validation, a
barrier-controlled two-editor race, permanent SKU ownership after archive, inherited
required attributes, and the rule that a schema-invalidated active product is revised
to `DRAFT` and disappears from the sellable view.
