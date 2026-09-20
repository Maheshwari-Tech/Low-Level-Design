# Facade Pattern

## Intent

Provide a small, coherent interface for a complex subsystem while leaving lower-level subsystem APIs available when needed.

## When to use

- A common workflow currently requires clients to coordinate many subsystem objects.
- Layers need a clear entry point and dependency boundary.
- A legacy or third-party subsystem should be easier and safer to consume.

## Participants and mechanics

- **Facade** exposes use-case-oriented operations and orchestrates subsystem calls.
- **Subsystem classes** perform the actual specialized work and need not know about the facade.
- **Clients** use the facade for common cases and may use subsystem APIs directly for advanced cases if policy allows.

## Trade-offs

- Reduces coupling and gives common workflows one place for sequencing and error handling.
- Can become a god object if unrelated use cases accumulate in one facade.
- It simplifies an interface but does not automatically isolate or replace the subsystem.

## Implementation status

**Runnable.** [`FacadeDemo.java`](FacadeDemo.java) exposes one checkout operation over inventory, payment, and shipping subsystem services.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/facade/FacadeDemo.java
java -cp "$out" code.structural.facade.FacadeDemo
```
