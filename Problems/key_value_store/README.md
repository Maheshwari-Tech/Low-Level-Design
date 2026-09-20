# Key-Value Store with Indexes

## Interview brief

Design a key-value store and an index, while preserving the source's separate RocksDB implementation prompt. Start with a single-process durable engine; replication, sharding, and wide-column semantics are follow-ups unless the interviewer explicitly adds them.

## Scope and variations

- `put`, `get`, and `delete` opaque values by key.
- Atomic write batches and compare-and-set by version.
- Create and maintain declared secondary indexes over decoded fields.
- Recover acknowledged writes after restart.
- Support snapshots/range iteration when the chosen ordered engine permits them.
- Discuss a RocksDB/LSM variation: write-ahead log, memtable, immutable sorted files, compaction, and bloom filters.

## Core model

`Key`, `Value`, `Version`, `Record`, `WriteBatch`, `StoreEngine`, `IndexDefinition`, `IndexEntry`, `Snapshot`, `WriteAheadLog`, `MemTable`, `SortedRun`, `Manifest`, and `CompactionPlan`.

The primary store owns truth. Secondary indexes are derived structures updated in the same commit boundary or repaired from an explicit durable change log.

## Invariants

- A successful key lookup returns one committed version or not found, never a partially written value.
- Every acknowledged durable write survives process restart under the stated durability mode.
- A batch exposes all of its writes atomically or none of them.
- A secondary index entry refers to the same committed record version as the primary value.
- Deletes create a tombstone until older sorted runs can no longer resurrect the value.
- Snapshot readers observe a stable version boundary while compaction rewrites physical files.
- Checksums detect corrupt log records and sorted blocks before returning data.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `put(key, value, expectedVersion)` | Insert/update with optional optimistic concurrency. |
| `get(key, snapshot)` | Read the newest visible value at a snapshot. |
| `delete(key, expectedVersion)` | Commit a tombstone rather than an unsafe physical removal. |
| `write(batch, durability)` | Atomically commit multiple mutations. |
| `createIndex(definition)` | Register an index and backfill from a known snapshot. |
| `queryIndex(index, predicate, cursor)` | Return keys/records with stable pagination. |
| `snapshot()` | Capture a consistent read boundary. |

## Key flows, concurrency, and failure

A write validates versions and indexable fields, appends one checksummed WAL record, applies the batch to the active memtable, and acknowledges according to the selected fsync policy. When the memtable freezes, a background flush writes a sorted immutable file and atomically advances the manifest.

Readers merge the memtable and candidate sorted runs, using bloom filters and sparse indexes to avoid unnecessary reads. Compaction selects overlapping runs, keeps the newest visible version, preserves required tombstones/snapshots, writes new files, then atomically swaps the manifest before deleting obsolete files.

Serialize mutations per key or commit sequence, not all reads. A crash during WAL append truncates the invalid tail; a crash before manifest swap leaves old files authoritative. Index creation records a snapshot plus subsequent changes so backfill cannot miss concurrent writes.

## Design decisions

- An LSM tree favors write throughput and sequential I/O at the cost of read and compaction amplification.
- A B-tree is a valid alternative for read-heavy/update-in-place workloads; choose from requirements.
- Keep indexing opt-in because arbitrary indexes multiply write cost and storage.
- State durability modes explicitly: memory-only, WAL-buffered, or fsync-before-acknowledgement.

## Follow-up questions

- Are range scans required, or only point reads?
- What key/value sizes, write/read ratio, and durability latency are expected?
- Can values be decoded for indexes, and how are schema changes handled?
- Are transactions multi-key, and what isolation level is required?
- When should replication, sharding, TTL, compression, or encryption be added?

## Provenance and implementation status

- Pinned prompts: [implement RocksDB](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L179) and [design a key-value store and an index](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L267).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
