# Single Responsibility Principle

## Principle

A module should have one coherent reason to change: it should own one responsibility for one set of stakeholders. A responsibility is a change axis, not necessarily a single method.

## Apply it when

- A class mixes domain rules with persistence, presentation, transport, or orchestration.
- Unrelated changes repeatedly touch the same file or require unrelated test setup.
- The class has low cohesion and collaborators use disjoint subsets of its behavior.

## Mechanics

- Identify the independently changing concerns and the stakeholders that request those changes.
- Keep the core policy in one cohesive unit and move separate concerns behind focused collaborators.
- Make dependencies explicit so orchestration composes the units without recreating the original coupling.

## Trade-offs

- Smaller cohesive units are easier to understand, test, and change independently.
- Splitting too early creates indirection, tiny pass-through classes, and fragmented workflows.
- Keep behavior together when it changes together, even if it performs several closely related steps.

## Implementation status

**Runnable.** [`InvoiceDemo.java`](InvoiceDemo.java) keeps calculation, formatting, and persistence in separate cohesive collaborators around an immutable invoice. The original working example remains in [`../solid.md`](../solid.md).

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Principles/single_responsibility/InvoiceDemo.java
java -cp "$out" InvoiceDemo
```
