# Coordination Service

## Interview brief

Design a ZooKeeper-like coordination service, preserving the source variation that explicitly asks how RocksDB and Google's BigTable ideas might participate. Separate the coordination contract from storage-engine choices: clients need ordered, fault-tolerant metadata operations; RocksDB can persist local replicas, while a BigTable-style store is not a substitute for consensus.

## Scope and variations

- Hierarchical names with small versioned values.
- Persistent, ephemeral, and sequential nodes.
- Session leases, watches, compare-and-set, and atomic multi-operations.
- Replicated writes with linearizable ordering and follower reads under a declared consistency mode.
- Local durable snapshots/logs through a storage-engine port; discuss RocksDB as one adapter.
- Treat building BigTable itself as a separate storage-system exercise, not hidden scope.

## Core model

`Path`, `ZNode`, `NodeVersion`, `Session`, `Lease`, `Watch`, `Transaction`, `Revision`, `ConsensusEntry`, `Replica`, `Snapshot`, and `StorageEngine`.

The replicated state machine owns logical truth. The local storage engine persists log/snapshot material but cannot independently accept conflicting writes.

## Invariants

- All successful mutations have one global revision order agreed by a quorum.
- A compare-and-set succeeds only against the expected node version.
- An ephemeral node exists only while its owning session lease remains valid.
- Sequential-node suffixes are unique and monotonic within their parent.
- An atomic multi-operation commits every mutation at one revision or none.
- A watch never invents state; clients may need to re-read because watches are one-shot/edge notifications.
- A fenced or minority replica cannot acknowledge writes.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `openSession(client, timeout)` | Establish a renewable lease and session identity. |
| `create(path, value, mode, session)` | Create persistent, ephemeral, or sequential metadata. |
| `get(path, consistency, watch?)` | Read value/version and optionally register a one-shot watch. |
| `set(path, value, expectedVersion)` | Perform a version-checked mutation. |
| `delete(path, expectedVersion)` | Remove an empty/version-matching node. |
| `multi(operations)` | Commit a small atomic coordination transaction. |
| `heartbeat(session, epoch)` | Renew the lease while rejecting stale owners. |

## Key flows, concurrency, and failure

The leader validates a mutation, appends it to the consensus log, replicates it to a quorum, commits the revision, applies it to the state machine, then returns. Concurrent writers to one node resolve through expected versions and the committed log order.

Session expiry is a replicated decision. The leader proposes expiry after the timeout; applying that revision deletes all owned ephemeral nodes and emits watch events. A partitioned old client is fenced by session epoch even if it later reconnects.

Followers restore a snapshot, replay later log entries, and serve reads only under the requested consistency guarantee. Local RocksDB failure affects one replica; quorum remains the availability boundary. Watch delivery is best-effort notification, so clients re-read and re-register after reconnect.

## Design decisions

- Use consensus for ordering and fencing; a shared database lock is not equivalent.
- Keep values small and coordination-focused rather than turning the service into a general object store.
- Model watches as hints over versioned state, avoiding an impossible exactly-once notification promise.
- Make session timeouts bounded and negotiated to balance failover speed against false expiry.

## Follow-up questions

- How many replicas, regions, clients, nodes, watches, and writes per second?
- Are follower/stale reads acceptable?
- What maximum value and transaction sizes are allowed?
- How are membership changes, snapshots, log compaction, and disaster recovery performed?
- Which recipes must be demonstrated: leader election, locks, configuration, or service discovery?

## Provenance and implementation status

- Pinned prompt: [develop ZooKeeper using RocksDB and BigTable](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L180).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
