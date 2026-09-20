# Document Collaboration Platform

## Interview brief

Design real-time collaboration without collapsing four source variations:

1. a general team collaboration application;
2. a visual workplace for remote teams;
3. an online UML diagram editor; and
4. a Google Docs-style service to view, add, and remove document viewers.

The fourth is primarily an authorization/membership problem; the first three add concurrent content editing, presence, and richer object types.

## Scope and variations

- Create documents/workspaces and manage owner, editor, commenter, and viewer access.
- Open collaborative sessions and broadcast presence/cursors ephemerally.
- Apply concurrent text or structured-object operations and recover offline clients.
- Maintain revision history, snapshots, comments, and audit events.
- Represent visual-board/UML nodes, connectors, geometry, and grouping without forcing them into plain text.
- Keep video meetings, project management, and arbitrary plugin execution outside the baseline.

## Core model

`Workspace`, `Document`, `DocumentRevision`, `ContentOperation`, `Participant`, `Membership`, `Permission`, `ShareGrant`, `Session`, `Presence`, `Comment`, `CanvasObject`, `DiagramNode`, `Connector`, and `Snapshot`.

Durable membership and content are separate from ephemeral presence. Text and diagram documents can share lifecycle/access infrastructure while using different operation strategies.

## Invariants

- Only a principal authorized at the accepted membership revision may read or mutate content.
- Add/remove-viewer commands are idempotent and versioned; removal fences future sessions/tokens promptly.
- Every accepted operation has a unique client operation ID and one server revision.
- Clients applying the same accepted operation set converge to the same document state under the chosen OT/CRDT contract.
- A snapshot plus subsequent ordered operations reconstructs one exact revision.
- Diagram connectors reference valid object identities or an explicit tombstone policy.
- Presence expiry never changes durable membership or content.
- Audit history identifies actor and action without storing sensitive content unnecessarily.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `createDocument(workspace, type, title, clientRequestId)` | Create text, board, or UML content. |
| `changeAccess(documentId, command, expectedMembershipVersion)` | Add/remove/change viewer/editor permissions. |
| `openSession(documentId, knownRevision, deviceId)` | Authorize and return snapshot/delta plus session epoch. |
| `submitOperations(sessionId, operations, baseRevision)` | Deduplicate, transform/merge, and append edits. |
| `changes(documentId, afterRevision, limit)` | Resume from a stable revision cursor. |
| `updatePresence(sessionId, ephemeralState)` | Broadcast bounded cursor/selection state. |
| `history(documentId, beforeRevision, limit)` | Read authorized revisions/audit evidence. |

## Key flows, concurrency, and failure

Opening a session verifies membership, returns a recent snapshot plus later operations, and issues a short-lived session epoch. A membership removal advances the access version and invalidates/fences sessions; gateways recheck on each mutation and receive revocation events.

For editing, choose OT with a central document sequencer or a CRDT with stable operation identities. A submission is deduplicated by client operation ID, validated against document type and permissions, merged/transformed, assigned a revision, persisted, then broadcast. Offline clients send queued operations after resync; rejected operations remain visible to the client for deliberate resolution.

Text edits, canvas moves, and UML connector changes have different conflict semantics. Preserve them as operation strategies: for example, concurrent scalar property updates may use a declared last-writer/version rule while collections use identity-aware insert/delete operations.

Gateways may drop broadcasts; clients recover from durable revision cursors. Presence uses TTL and lossy updates. Snapshot generation is asynchronous and must not delete operations still required by active/offline recovery policy.

## Design decisions

- Keep access control authoritative and independent from collaboration transport.
- Partition by document ID to give each document a clear operation-order owner when using OT.
- Use operation IDs and revision cursors for retry/resume rather than exactly-once sockets.
- Model visual/UML objects explicitly so relationships and referential integrity remain testable.

## Follow-up questions

- Is content text, rich text, free-form canvas, UML, or all of them?
- Which OT/CRDT/offline guarantees and maximum collaborator count are required?
- Can removed viewers retain cached/offline content, and what can revocation realistically guarantee?
- Are public links, domain sharing, comments, suggestions, exports, or version restore required?
- What history retention, encryption, residency, and audit obligations apply?

## Provenance and implementation status

- Pinned prompts: [real-time team collaboration](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L129), [visual remote workplace](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L130), [online UML diagram tool](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L131), and [view/add/remove document viewers](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L190).
- The pinned primer has no solution-index row or local implementation for these topics.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
