# Multi-Level Cache

## Problem

Design an `N`-level key-value cache in which L1 is fastest/highest priority and Ln is slowest/lowest priority. Each level has its own capacity, read cost, write cost, storage, and eviction policy.

## Operations

### Read

Search from L1 downward. On a hit, return the value and promote it into every missed higher-priority level. The reported cost is the read cost of searched levels plus the write cost of promotion.

### Write

Write the key/value through the configured levels. A level may skip a physical write when it already contains the same value. Define whether an eviction or lower-level failure aborts, continues, or compensates earlier writes.

### Statistics

Report filled/capacity for each level and rolling average latency for the last ten reads and writes. Statistics collection must not change cache correctness.

## Design

`MultiLevelCacheService` coordinates a chain of `ILevelCache` nodes. Each level composes storage and eviction policies; response records retain value, latency, and usage information. A null-object tail terminates traversal without repeated null checks.

## Invariants and edge cases

- A hit at level `k` is promoted to every level `1..k-1` according to capacity policy.
- Values visible in higher levels must not contradict a completed write to lower levels.
- Handle missing keys, zero-capacity levels, eviction during promotion, repeated values, partial failure, and concurrent reads/writes of one key.

## Local implementation

The Java example under [`com/example/lld/multi_level_cache/code`](com/example/lld/multi_level_cache/code/) includes cache levels, service orchestration, response models, storage, LRU/LFU/random eviction policies, a null-object level, and a `Main` demo.

See [Cache](../cache/) for the single-level building block.
