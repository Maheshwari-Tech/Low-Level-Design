# Iterator Pattern

## Intent

Traverse a collection in a defined order without exposing the collection's internal representation.

## When to use

- Clients need sequential access but should not depend on storage details.
- A collection needs multiple traversal orders or filters.
- Traversal state should live outside the collection so several traversals can proceed independently.

## Participants and mechanics

- **Iterator** exposes operations such as `hasNext` and `next`.
- **Concrete iterator** stores the current traversal position and order.
- **Aggregate** exposes a factory for an iterator.
- **Concrete aggregate** creates an iterator that understands its private representation.
- The client advances only through the iterator contract.

## Trade-offs

- Separates traversal from storage and supports multiple concurrent traversal objects.
- Mutation during traversal requires an explicit snapshot, fail-fast, or weak-consistency policy.
- A custom iterator is unnecessary when the language's collection iterator already expresses the required traversal.

## Implementation status

**Runnable.** [`IteratorDemo.java`](IteratorDemo.java) exposes a snapshot iterator over a playlist without revealing its mutable storage.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/iterator/IteratorDemo.java
java -cp "$out" code.behavioral.iterator.IteratorDemo
```
