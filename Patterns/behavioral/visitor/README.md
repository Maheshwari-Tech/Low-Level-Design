# Visitor Pattern

## Intent

Add operations to a stable object structure without placing every operation inside each element class.

## When to use

- The element types are stable but new operations are added frequently.
- An operation needs type-specific behavior across a heterogeneous structure.
- Related operations should be grouped separately from domain elements.

## Participants and mechanics

- **Element** defines `accept(visitor)`.
- **Concrete elements** call the visitor overload for their own type; this is double dispatch.
- **Visitor** declares one visit operation per concrete element type.
- **Concrete visitors** implement an operation across all element types.
- The client traverses the structure and asks each element to accept a visitor.

## Trade-offs

- Makes new cross-cutting operations easy to add and keeps them together.
- Adding a new element type requires changing every visitor.
- Visitors may need accessors that expose more element state than ordinary clients should see.

## Implementation status

**Runnable.** The [`good`](good/) hotel-room example separates pricing, booking, and maintenance visitors from room elements; [`RoomElement.java`](good/RoomElement.java) and [`RoomVisitor.java`](good/RoomVisitor.java) show double dispatch. [`Main.java`](good/Main.java) applies every operation to every room. The [`bad`](bad/) version places unrelated operations directly on room classes for comparison.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/visitor/good/*.java
java -cp "$out" code.behavioral.visitor.good.Main
```
