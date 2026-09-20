# Builder Pattern

## Intent

Construct a complex object incrementally while keeping its assembly process separate from its final representation.

## When to use

- A constructor would have many optional or easy-to-confuse parameters.
- The same assembly steps should create different representations.
- Creation must enforce ordering, defaults, or validation before an object becomes usable.

## Participants and mechanics

- **Product** is the object being assembled.
- **Builder** exposes focused configuration or construction steps.
- **Concrete builder** stores intermediate state and creates the product in `build()`.
- An optional **director** applies a reusable sequence of steps.
- The client configures a builder and receives the validated product only after construction is complete.

## Trade-offs

- Improves readability and can preserve product immutability.
- Centralizes defaults and validation.
- Adds another type and may be excessive for small objects with few stable parameters.

## Implementation status

**Runnable.** [`BuilderDemo.java`](BuilderDemo.java) builds an immutable HTTP request with defaults, fluent optional headers, defensive copying, and validation at `build()`.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/creational/builder/BuilderDemo.java
java -cp "$out" code.creational.builder.BuilderDemo
```
