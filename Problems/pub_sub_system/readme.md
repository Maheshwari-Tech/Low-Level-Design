# Publish-Subscribe System

## Problem

Design a topic-based messaging system in which publishers and subscribers remain decoupled. Publishers append messages to topics; subscribers consume the topics they choose at their own pace.

## Requirements

- Create and discover topics.
- Subscribe and unsubscribe consumers from a topic.
- Publish a message without knowing its consumers.
- Fan out each message to every active subscription.
- Define ordering, persistence, acknowledgement, retry, and delivery guarantees.
- Isolate slow consumers with queues and backpressure.
- Support concurrent publishers and subscribers safely.

“Real time,” “persistent,” and “guaranteed” are not implementation details: the design must choose semantics such as at-most-once, at-least-once, or effectively-once processing.

## Core Model

- **`PubSubSystem`**: client-facing facade for topic and subscription operations.
- **`MessageBroker`**: routes publications and coordinates delivery.
- **`Topic`**: named append stream or channel.
- **`Message`**: immutable ID, topic, payload, timestamp, and optional key/headers.
- **`Publisher`**: sends messages to a topic.
- **`Subscriber`**: callback or poll-based consumer.
- **`Subscription`**: subscriber/topic relationship, delivery cursor, and retry state.

## Data Structures

- `Map<String, Topic>` for the topic registry.
- `Map<String, Set<Subscription>>` for subscribers per topic.
- A `Queue<Message>` per topic for simple transient delivery, or an append-only log plus per-subscription offsets for replay and durable consumption.
- `ConcurrentHashMap` and thread-safe queues for an in-process concurrent broker.
- A delayed priority queue for retry scheduling.

Per-topic queues alone are insufficient for independent consumers if removing a message hides it from other subscribers. Fan-out queues or a shared immutable log with independent cursors preserve pub-sub semantics.

## Patterns

- **Observer** models topic-to-subscriber notification.
- **Mediator** keeps publishers and subscribers from coordinating directly.
- **Factory** can validate construction of topics, messages, and subscriptions.
- **Strategy** is useful for retry, partitioning, and dispatch policies.

## Publish and Delivery Flow

1. Validate the topic and assign an immutable message ID.
2. Persist or enqueue the message before acknowledging the publisher, according to the chosen durability contract.
3. Dispatch it to every matching subscription while preserving the documented ordering boundary.
4. Advance a subscription cursor only after successful acknowledgement.
5. Retry or dead-letter failed deliveries without blocking unrelated subscribers.

## Implementation Status

This target currently contains the design document only. The central broker, concurrent queues, durable log, delivery guarantees, retry/dead-letter handling, persistence, and scaling/partitioning are proposed components and are not implemented here.
