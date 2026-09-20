# Prototype Pattern

## Intent

Create a new object by copying an existing, configured object rather than rebuilding it from scratch.

## When to use

- Object creation is expensive or requires substantial configuration.
- The required concrete type is known only at runtime.
- A set of preconfigured templates should be cloned and then customized.

## Participants and mechanics

- **Prototype** defines a copy operation.
- **Concrete prototype** decides how its state is copied.
- The client obtains a prototype, clones it, and modifies only the differing state.
- The copy policy must state whether nested mutable objects are shared (shallow copy) or copied recursively (deep copy).

## Trade-offs

- Avoids repeated setup and can reduce dependence on concrete constructors.
- Makes runtime registration of new templates straightforward.
- Correct copying of mutable graphs, identities, and external resources can be difficult.

## Implementation status

**Runnable.** [`PrototypeDemo.java`](PrototypeDemo.java) copies a configured document and defensively copies its mutable section list so the clone can change independently.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/creational/prototype/PrototypeDemo.java
java -cp "$out" code.creational.prototype.PrototypeDemo
```
