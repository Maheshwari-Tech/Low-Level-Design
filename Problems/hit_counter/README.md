# Hit Counter

## Interview Prompt

Design a counter that records timestamped hits and returns the total over a rolling window, optionally partitioned by key. Make boundary semantics and concurrent updates explicit.

## Requirements

1. Record one or a weighted number of hits for a key at a supplied instant.
2. Return hits in the half-open window `(now - window, now]`.
3. Support high concurrent write throughput and bounded memory.
4. Reject negative weights and define behavior for late/out-of-order timestamps.
5. Evict inactive keys without losing still-queryable buckets.

## Model and Invariants

Use `CounterKey`, `TimeBucket`, `BucketRing`, `Clock`, and a `HitCounter` service. A ring has `window / resolution` slots; every slot stores its bucket epoch and count. Before adding or reading a slot, compare the stored epoch and reset stale data atomically.

- Counts are non-negative and a bucket belongs to exactly one epoch.
- An expired bucket contributes zero even if its array slot was reused.
- Concurrent writers cannot lose increments.
- The same timestamp/window convention is used by write, read, and tests.

```java
void hit(CounterKey key, long weight, Instant occurredAt);
long count(CounterKey key, Duration window, Instant now);
void evictIdle(Instant idleBefore);
```

## Design Variations

- **Exact deque:** store every timestamp; simple but memory grows with traffic.
- **Bucket ring:** bounded memory and small approximation error determined by resolution.
- **External atomic store:** use time buckets in Redis or a database for multi-process counters, with TTL and an atomic increment script.

Keep per-key state independent so unrelated counters do not share one global lock. Inject time and test the exact expiry boundary.

## Source-Backed Fragment

- `References/kumaransg-LLD/Low_level_Design_Problems/HitCounter/` — Java counter service with a Redis abstraction. The source is a useful fragment, not a production-complete solution. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/HitCounter)

## Follow-Ups

Unique visitors, top keys, multi-resolution windows, sharded aggregation, late data, and approximate sketches.

## Implementation Status

The canonical page completes the interview reasoning around the upstream fragment; the actual Java remains in the unmodified clone.
