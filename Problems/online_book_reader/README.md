# Online Book Reader

## Interview Prompt

Design a reading application that gives entitled users access to digital books, remembers their position, and supports bookmarks and a personal library. This is intentionally separate from the retail-focused [Online Bookstore](../online_bookstore/README.md).

## Scope and Requirements

1. Add immutable book editions and ordered readable content units.
2. Grant/revoke a user's entitlement to an edition.
3. Open an entitled book at the last synchronized position.
4. Move pages/locations, add notes or bookmarks, and list reading progress.
5. Synchronize progress from multiple devices without silently moving a user backward.
6. Keep content delivery behind a port; the aggregate stores references, not large binaries.

## Core Model

`Book`, `Edition`, `ContentLocation`, `Entitlement`, `ReadingSession`, `ReadingProgress`, `Bookmark`, `Annotation`, `LibraryRepository`, and `ContentPort` form the core. Treat edition identity and location scheme as immutable so saved progress remains meaningful after catalog updates.

## Invariants

- A user opens content only with a live entitlement.
- A bookmark targets an existing location in the entitled edition.
- A progress update includes device sequence/version and cannot overwrite a causally newer update.
- Removing an item from the visible library does not erase purchase or audit history.

## API Sketch

```java
ReadingSession open(UserId user, EditionId edition, DeviceId device);
Progress updateProgress(SessionId session, long expectedVersion, ContentLocation location);
Bookmark addBookmark(UserId user, EditionId edition, ContentLocation location, String note);
List<LibraryItem> library(UserId user, PageCursor cursor);
ContentPage read(SessionId session, ContentLocation location);
```

## Design Solution

Keep entitlement and reading progress as authoritative metadata; retrieve/decrypt page content through an injected adapter. Use optimistic versions per `(user, edition)` and a documented merge policy for offline devices—usually furthest-progress wins only for automatic position, while explicit user jumps carry a newer logical clock. Cache content pages independently from progress. Publish progress events for analytics without placing analytics in the reading transaction.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/System-design/online_book_reader_system/` — Java reader-oriented model. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/System-design/online_book_reader_system)
2. `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem/` — single-file nested design fragment. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem)
3. `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem2/` — alternate single-file design; preserve it separately. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem2)

## Follow-Ups

Offline downloads and DRM, highlights spanning locations, shared family libraries, audiobook positions, accessibility settings, and content-version migration.

## Implementation Status

This page supplies the maintained interview solution. The three actual Java variations remain in the unmodified local clone; two are explicitly fragments.
