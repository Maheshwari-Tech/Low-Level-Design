# Adapter Pattern

## Intent

Convert an existing interface into the interface a client expects so otherwise incompatible types can collaborate.

## When to use

- Integrating a legacy or third-party API without changing its source.
- Presenting one stable application contract over several vendor-specific interfaces.
- Isolating format or protocol translation at a system boundary.

## Participants and mechanics

- **Target** is the interface expected by the client.
- **Adaptee** is the useful type with an incompatible interface.
- **Adapter** implements the target and translates calls or data to the adaptee.
- **Client** depends only on the target. Object adapters usually compose an adaptee; class adapters use inheritance where the language permits it.

## Trade-offs

- Reuses existing code while keeping conversion logic out of clients.
- Adds an indirection layer and may leak mismatched semantics when the interfaces are not truly compatible.
- Two-way adapters and many special cases can become difficult to maintain.

## Implementation status

**Runnable.** [`AdapterDemo.java`](AdapterDemo.java) adapts a decimal `PaymentGateway` contract to a legacy bank API that accepts integer cents. The historical folder name `adaptor` is retained for path compatibility.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/adaptor/AdapterDemo.java
java -cp "$out" code.structural.adaptor.AdapterDemo
```
