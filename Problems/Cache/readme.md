# Cache

## Problem

Design an extensible, generic key-value cache with configurable capacity. The cache should separate storage from eviction so either can evolve independently while keeping `get` and `put` fast.

## Requirements

- Store and retrieve generic key-value pairs.
- Enforce a fixed capacity.
- Update recency when a key is read or written.
- Evict one key when a new entry arrives at capacity.
- Support pluggable storage backends and eviction policies.
- Report missing keys and full-storage failures consistently.
- Keep thread safety, TTL expiry, persistence, and distributed coordination as explicit extensions.

## Core Design

- **`Cache` / `ICache`**: public `get`, `put`, and, in the packaged variant, `delete` contract.
- **`CacheImpl`**: coordinates storage and eviction; it does not own policy-specific logic.
- **`Storage` / `IStorage`**: storage abstraction.
- **`HashMapBasedStorage`**: capacity-bounded in-memory implementation.
- **`EvictionPolicy` / `IEvictionPolicy`**: records accesses and selects an eviction candidate.
- **`LRUBasedEvictionPolicy` / `LRUEvictionPolicy`**: LRU bookkeeping.
- **`DoublyLinkedList` and `DoublyLinkedListNode`**: constant-time recency updates when paired with a node index.
- **`CacheFactory`**: assembles a cache from a storage backend and policy.
- **`NotFoundException`, `KeyNotFoundException`, and `StorageFullException`**: failure contracts used by the two educational variants.

The main structures are a `HashMap<Key, Value>` for average O(1) lookup and a `HashMap<Key, Node>` plus a doubly linked list for average O(1) LRU access, removal, and eviction.

## Patterns and Trade-offs

- **Strategy** isolates eviction and storage choices from `CacheImpl`.
- **Factory** centralizes valid default combinations.
- A template-method-style base cache could standardize future instrumentation or loading hooks, but the current code uses composition rather than a Template Method hierarchy.
- In-memory maps favor latency and simplicity; they do not provide durability or cross-process coherence.

## Repository Layout and Status

This directory contains two educational implementations:

- `com/example/lld/cache/code/` is the more structured variant. Its LRU policy is implemented; LFU and random policy classes are placeholders and must not be selected as complete policies.
- `com/example/lld/cache/` is an older default-package draft with the same storage/policy split. Its `Main` is only a placeholder demo, so treat this variant as design practice rather than a production-ready cache.

Neither variant currently implements thread safety, TTL, asynchronous loading, metrics, persistence, or distributed invalidation. A useful next step is a behavioral test suite covering overwrite-at-capacity, repeated access, deletion, missing keys, and eviction order.

## Run

There is no Maven module in this target directory. From `Problems/cache/com/example/lld/cache`, the packaged variant can be compiled with:

```bash
javac code/*.java code/algorithms/*.java code/exception/*.java code/policies/*.java code/storage/*.java
java code.Main
```

The current `Main` verifies wiring only; it is not yet an end-to-end cache scenario.
