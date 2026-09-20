# Chat and Messaging Service

## Interview brief

Design a WhatsApp/WeChat-style chat application. Begin with durable one-to-one and group messaging across multiple user devices. Calls, social feeds, payments, mini-programs, and end-to-end encryption protocol construction are separate extensions.

## Scope and variations

- Create direct and group conversations with membership roles.
- Send text and attachment references with client-generated idempotency IDs.
- Synchronize messages across a user's devices.
- Track server acceptance, delivery, read progress, and conversation unread counts.
- Support offline catch-up, pagination, edits/deletes under policy, and push-notification hints.
- State whether message order is per conversation and whether end-to-end encryption is required.

## Core model

`User`, `Device`, `Conversation`, `Membership`, `Message`, `MessageId`, `ConversationSequence`, `Attachment`, `DeliveryReceipt`, `ReadCursor`, `DeviceCursor`, and `EncryptionEnvelope`.

A message is immutable content plus status metadata. Read state is a per-member cursor rather than one row per message whenever the ordering contract permits it.

## Invariants

- A client message ID is unique within its sender/device namespace and retries create at most one logical message.
- Only a member authorized at the send revision can append to a conversation.
- Server sequence numbers are unique and monotonic per conversation; no global order is promised.
- Attachment references are usable only after upload/scanning policy succeeds.
- A read cursor never moves backwards.
- Removing a member prevents future access while preserving the declared history policy.
- Delivery/read receipts never claim stronger evidence than the system observed.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `createConversation(kind, members, clientRequestId)` | Create direct/group chat idempotently. |
| `sendMessage(conversationId, clientMessageId, content, replyTo)` | Append once and return assigned sequence. |
| `messages(conversationId, beforeSequence, limit)` | Read a stable ordered page. |
| `sync(deviceId, afterCursor, limit)` | Catch a device up across conversations. |
| `ackDelivered(deviceId, messageIds)` | Record bounded delivery evidence. |
| `markRead(conversationId, throughSequence)` | Advance the member's read cursor. |
| `changeMembership(conversationId, command, expectedVersion)` | Add/remove/promote with optimistic concurrency. |

## Key flows, concurrency, and failure

The send path authenticates device and membership, deduplicates on client message ID, allocates the next conversation sequence at the partition owner, persists the message, and emits delivery work through an outbox. Sender retries receive the original message identity.

Recipients connected to a gateway receive a hint/event and advance durable device cursors after fetch/ack. Offline devices later sync from their cursor. Push notifications contain minimal metadata and are not the durable message channel.

Concurrent group membership changes use a conversation version. A send racing removal is accepted or rejected according to the single committed order. Large groups may use fan-out-on-read or hybrid delivery rather than materializing one copy per recipient.

If end-to-end encryption is required, the server stores opaque per-device envelopes, manages no plaintext, and still owns routing/order metadata. Key verification, membership changes, backup, and multi-device rekeying need a separately reviewed protocol.

## Design decisions

- Partition by conversation ID to obtain a clear per-chat ordering owner.
- Use client IDs for idempotency and server sequences for pagination/order.
- Represent delivery/read state compactly with device/member cursors where possible.
- Store attachment bytes outside the message database behind immutable references.

## Follow-up questions

- What group size, message rate, retention, and multi-region latency are expected?
- Is end-to-end encryption mandatory, and may history sync to a newly added device?
- Are edits, deletes-for-everyone, reactions, threads, mentions, and disappearing messages required?
- What exactly do sent, delivered, and read mean across multiple devices?
- How are abuse, blocking, legal retention, and user deletion handled?

## Provenance and implementation status

- Pinned prompt: [chat application like WhatsApp/WeChat](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L87).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
