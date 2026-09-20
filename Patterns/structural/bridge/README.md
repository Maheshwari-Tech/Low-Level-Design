# Bridge Pattern

## Intent

Separate an abstraction from the implementation that performs its work so both dimensions can evolve independently.

## When to use

- Two independent axes of variation would otherwise create a subclass explosion.
- An abstraction must switch implementations at runtime.
- Platform-specific details should remain behind a stable domain API.

## Participants and mechanics

- **Abstraction** defines the client-facing API and holds an implementor reference.
- **Refined abstractions** extend domain behavior without selecting a concrete implementation.
- **Implementor** defines primitive operations needed by the abstraction.
- **Concrete implementors** supply platform- or strategy-specific behavior.
- The abstraction delegates implementation work through composition.

## Trade-offs

- Allows both hierarchies to change independently and favors composition over inheritance.
- Adds indirection and requires identifying the two variation dimensions early.
- It is unnecessary when only one dimension is expected to vary.

## Implementation status

**Runnable.** [`LivingThing.java`](LivingThing.java) is the abstraction side, with [`Dog`](Dog.java), [`Fish`](Fish.java), and [`Tree`](Tree.java) as refinements. [`BreathingProcess.java`](BreathingProcess.java) is the independently injected implementor side. [`BridgeDemo.java`](BridgeDemo.java) composes the two dimensions.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/bridge/*.java
java -cp "$out" code.structural.bridge.BridgeDemo
```
