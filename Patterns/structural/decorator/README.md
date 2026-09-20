# Decorator Pattern

## Intent

Add behavior to an individual object by wrapping it with objects that implement the same contract.

## When to use

- Responsibilities must be combined dynamically or per instance.
- Subclassing every combination would create a large, rigid hierarchy.
- Added behavior should be removable or reorderable by changing the wrapper chain.

## Participants and mechanics

- **Component** defines the shared operation.
- **Concrete component** supplies the base behavior.
- **Decorator** implements the component contract and holds another component.
- **Concrete decorators** delegate to the wrapped component and add work before, after, or around that call.
- The client composes a wrapper chain and uses it as one component.

## Trade-offs

- Supports flexible combinations without modifying the wrapped class.
- Can create many small objects; behavior may depend on wrapper order.
- Debugging and identity checks become harder because the visible object is a chain.

## Implementation status

**Runnable.** The canonical [`pizzaExample/good`](pizzaExample/good/) implementation composes toppings around a base pizza; [`Main.java`](pizzaExample/good/Main.java) builds and prices two chains. A historical parallel copy remains under `good/` to preserve user code but is not a separate example. Additional notes are in [`decorator.md`](decorator.md).

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/decorator/pizzaExample/good/*.java
java -cp "$out" patterns.structural.decorator.pizzaExample.good.Main
```
