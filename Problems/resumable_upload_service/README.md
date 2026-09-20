# Resumable Upload Service

## Interview brief

Design upload for a video larger than 1 GB over an unreliable, low-bandwidth network where connections repeatedly drop around halfway. Preserve the separate learning-app variation requiring an upload to continue when a browser is closed.

The latter needs an honest platform boundary: a normal closed browser cannot be guaranteed to keep executing indefinitely. A web design persists multipart state and resumes on reopen; limited service-worker/background-transfer APIs may help where supported. A native uploader or server-side transfer agent is needed for a strong “continues while closed” guarantee.

## Scope and variations

- Initiate a multipart upload and negotiate part size/checksum rules.
- Upload parts independently, out of order, and directly to object storage when appropriate.
- Query durable progress and resume without retransmitting verified parts.
- Complete only after the full manifest validates.
- Abort/expire abandoned sessions and clean orphaned parts.
- Publish processing status and notify a teacher/owner after upload completion in the learning variation.

## Core model

`UploadSession`, `UploadObject`, `PartPlan`, `UploadedPart`, `Checksum`, `UploadManifest`, `ClientCheckpoint`, `StorageUploadId`, `Completion`, `ProcessingJob`, and `Notification`.

The service stores metadata and authority; object storage owns bytes. A completed object is immutable and separate from post-upload transcoding/scanning state.

## Invariants

- An uploaded part is identified by session, part number/range, length, and checksum.
- Retrying identical part content is idempotent; conflicting content for the same part is rejected or creates an explicit replacement version.
- Reported progress counts only durably verified parts.
- Completion succeeds once and only when the manifest covers the declared object exactly without gaps/overlap.
- The final object checksum/size matches the initiated contract before it becomes available.
- Expired/aborted sessions cannot accept or complete new parts.
- Notification or transcoding failure does not invalidate a successfully stored upload.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `initiate(fileMetadata, checksumPolicy, idempotencyKey)` | Create a durable session and part plan. |
| `authorizePart(sessionId, partNumber, checksum)` | Issue a short-lived upload target. |
| `recordPart(sessionId, partNumber, storageTag, checksum)` | Idempotently verify/store part metadata. |
| `status(sessionId)` | Return verified parts, expiry, and next work. |
| `complete(sessionId, orderedManifest, idempotencyKey)` | Validate and atomically publish the completed object. |
| `abort(sessionId, reason)` | Fence the session and schedule cleanup. |
| `processingStatus(objectId)` | Read scanning/transcoding/notification progress. |

## Key flows, concurrency, and failure

The client persists the session ID, file fingerprint, and verified part map. It uploads bounded parts with retry/backoff, asks status after reconnect, hashes only missing/corrupt parts, and continues. Smaller parts reduce retransmission; larger parts reduce request/metadata overhead. Adaptive concurrency must respect low bandwidth rather than multiplying contention.

Part acknowledgements use a unique `(sessionId, partNumber)` key. Concurrent retries with the same checksum return the stored result; a checksum mismatch is explicit. Completion conditionally changes `OPEN -> COMPLETING -> COMPLETE`, delegates multipart assembly, verifies final evidence, and reconciles an unknown storage timeout before retrying.

For a browser, checkpoints live in durable browser storage and bytes remain user-selected/file-system handles subject to permission. A service worker may continue briefly, but the portable guarantee is resume-on-reopen. A desktop/mobile background uploader can keep a trusted file handle and continue under OS policy.

After completion, an outbox schedules scanning/transcoding and teacher notification. Processing is idempotent by object/version.

## Design decisions

- Upload chunks directly to object storage using short-lived scoped URLs to avoid proxying gigabytes through the application service.
- Use checksums per part plus optional whole-object verification.
- Make progress server-authoritative and client checkpoints reconstructible.
- Separate byte upload, object publication, media processing, and notification states.

## Follow-up questions

- Which clients and background-transfer guarantees are required?
- What maximum size, network conditions, part limits, and expiry apply?
- Must files be encrypted client-side or deduplicated?
- When is an upload visible: after storage, malware scan, or transcoding?
- How are quota, ownership, orphan cleanup, and region residency enforced?

## Provenance and implementation status

- Pinned prompts: [large video over unreliable low bandwidth](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L58-L60) and [learning-app upload continuing after browser closure](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L207-L209).
- The pinned primer has no solution-index row or local implementation for these topics.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
