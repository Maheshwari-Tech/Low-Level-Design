# URL Shortener

## Prompt and scope

Design the core of a URL shortener: accept a long URL, allocate an ID, encode it as a compact alias, and resolve that alias back to the stored URL. The source contributes only a deterministic integer-to-base-66 codec and its inverse; persistence and HTTP behavior are design work, not implemented source features.

Analytics, custom aliases, expiry, abuse scanning, and multi-region replication are follow-ups.

## Core model

- `ShortLink`: alias, normalized destination URL, creation time, optional expiry, owner, and status.
- `AliasCodec`: bijection between a non-negative numeric ID and an alphabet.
- `LinkRepository`: unique alias-to-link mapping and ID allocation.
- `ShortenerService`: create and resolve use cases.
- Optional `AliasPolicy`: generated versus custom alias validation.

## Invariants

- Every active alias resolves to at most one destination; creation is atomic with uniqueness enforcement.
- The codec alphabet has unique characters and its declared base equals its length.
- `decode(encode(id)) == id` for every supported non-negative ID without overflow.
- Invalid characters, empty aliases, malformed destinations, expired links, and disabled links are distinct outcomes.
- Retries with the same idempotency key do not create multiple aliases.

## API

```text
create(longUrl, options, idempotencyKey) -> ShortLink
resolve(alias) -> RedirectTarget | NotFound | Gone
disable(alias, owner)
get(alias) -> LinkMetadata
```

The source codec methods are `idToShortUrl(int)` and `shortUrltoID(String)`.

## Main flow

1. Validate and normalize the destination URL.
2. Allocate a durable ID or generate an alias with collision handling.
3. Encode the ID and atomically store the mapping.
4. Resolve by validating the alias, reading the mapping, checking status/expiry, and returning a redirect target.
5. Emit analytics asynchronously so resolution latency does not depend on counters.

## Concurrency and failure handling

- ID generation and alias insertion must remain unique across workers; use a database sequence/range allocator or conditional insert.
- Cache only active mappings and invalidate on disable or expiry. Negative caching must be short-lived.
- Define behavior for repository timeouts, partially completed creation, hot aliases, and malicious destinations.
- Use `long` or an arbitrary-size value and checked arithmetic; the source uses `int`.

## Design solution

Separate the pure alias codec from storage and application policy. Deterministic ID encoding avoids random collisions but exposes approximate creation order; obfuscate IDs or use a random alias policy when enumeration matters. Keep redirects read-optimized, with a source-of-truth repository and optional cache.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Codec fragment | [`References/kumaransg-LLD/Low_level_Design_Problems/UrlShortern/TinyUrlShortern.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/UrlShortern/TinyUrlShortern.java) | [file at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/UrlShortern/TinyUrlShortern.java) | Base-66 integer encode/decode demonstration; no link repository or service. |

No second implementation or exact duplicate is present in the clone for this question.

## Follow-ups

- Compare sequential, Snowflake-style, random, and hash-derived aliases.
- Add custom aliases, expiry, ownership, deletion, and redirect-code policy.
- Design cache invalidation, analytics ingestion, and hot-key protection.
- Prevent open-redirect abuse with validation, reputation, and rate limits.

## Implementation status

**Runnable codec fragment; no code copied here.** The source compiles and round-trips its example ID `200`, but it is not a URL-shortening service. It uses an `int`, accepts a punctuation alphabet, does not robustly reject unknown characters, and its punctuation decoder is not a general inverse for the declared alphabet. There are no tests, persistence, or URL APIs.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
