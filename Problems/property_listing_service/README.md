# Property Listing / Property Hunt Service

## Interview Prompt

Design a marketplace where owners list properties and buyers search, rank, shortlist, and mark a listing as sold. Preserve measurements and money precisely while supporting filters expressed in different units.

## Scope and Requirements

1. Register users and create a property listing with location, price, size, room count, and amenities.
2. Search by locality, price range, area range, room count, listing type, and availability.
3. Normalize square-foot, square-yard, and square-metre queries without losing the entered unit.
4. Sort results deterministically and paginate with a stable cursor.
5. Add or remove a listing from a user's shortlist idempotently.
6. Move a listing through `DRAFT`, `ACTIVE`, `SOLD`, and `WITHDRAWN` with authorization and version checks.

## Core Model

| Type | Responsibility |
| --- | --- |
| `PropertyListing` | Aggregate root for seller-owned listing data and lifecycle |
| `Address` / `GeoPoint` | Structured locality and optional coordinates |
| `Area` | Decimal magnitude plus source unit and normalized comparison value |
| `Money` | Currency plus exact minor-unit amount |
| `SearchCriteria` | Optional typed filters, sort, and cursor |
| `ShortlistEntry` | Unique `(userId, listingId)` bookmark |
| `ListingRepository` | Versioned writes and authoritative reads |
| `SearchIndex` | Denormalized discovery projection rebuilt from listing events |

## Invariants

- Only the owner or an authorized moderator changes a listing.
- An `ACTIVE` listing has a positive price and area plus enough searchable location data.
- Unit conversion uses a declared precision and rounding policy; display retains the original unit.
- `(userId, listingId)` appears at most once in a shortlist.
- `SOLD` and `WITHDRAWN` listings never appear in default active search results.

## API Sketch

```java
ListingId createListing(CreateListing command, String idempotencyKey);
PropertyListing update(ListingId id, long expectedVersion, ListingPatch patch);
PropertyListing transition(ListingId id, long expectedVersion, ListingStatus target);
SearchPage search(SearchCriteria criteria, PageCursor cursor);
void shortlist(UserId userId, ListingId listingId);
void removeFromShortlist(UserId userId, ListingId listingId);
```

## Design Solution

Keep lifecycle truth in the listing aggregate and publish a versioned `ListingChanged` event. A search projection stores normalized area, price, locality tokens, and status for fast filtering. Search may be eventually consistent, but an attempt to contact or purchase must re-read the authoritative listing. Use optimistic concurrency for seller edits and an idempotency key for create. A unique shortlist key turns repeated bookmark requests into success.

## Source-Backed Solution Variation

- `References/kumaransg-LLD/PropertyHunt/` — the actual Java/IntelliJ project with requirements, screenshots, listing, search, shortlist, unit conversion, and sale-state behavior. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/PropertyHunt)

This is distinct from [Product Search System](../product_search_system/README.md): Product Search focuses on generic relevance and facets, while Property Hunt owns marketplace listing state and domain-specific measurements.

## Interview Follow-Ups

- Geospatial radius and commute-time search.
- Duplicate/fraud detection and moderation.
- Saved-search alerts.
- Multi-currency pricing and historical price changes.
- Rental availability and visit scheduling.

## Implementation Status

The canonical page supplies the maintained interview solution. The unmodified upstream Java variation is available in the exact local clone and searchable from the website Source Library.
