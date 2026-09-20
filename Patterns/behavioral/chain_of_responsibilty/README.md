# Chain of Responsibility Pattern

## Intent

Pass a request through an ordered set of handlers until one handles it, or allow several handlers to contribute to its processing.

## When to use

- More than one object may handle a request and the sender should not select one directly.
- Handler order or membership must be configurable.
- Processing naturally forms a pipeline, such as validation, middleware, approval, or logging.

## Participants and mechanics

- **Handler** defines the request operation and a reference to the next handler.
- **Concrete handlers** either process the request, forward it, or do both according to the chain's policy.
- **Client** assembles the chain and submits a request to its first handler.
- The design must define what happens at the end: success, a no-op, a default handler, or an error.

## Trade-offs

- Decouples senders from concrete receivers and makes pipelines easy to reorder or extend.
- Control flow becomes distributed, which can make tracing and error handling harder.
- A request can go unhandled or be handled more than once unless the contract is explicit.

## Implementation status

**Runnable.** [`ChainOfResponsibilityDemo.java`](ChainOfResponsibilityDemo.java) builds authentication, role, and spending-limit handlers and sends accepted and rejected requests through the chain. The historical folder name `chain_of_responsibilty` is retained for path compatibility.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/chain_of_responsibilty/ChainOfResponsibilityDemo.java
java -cp "$out" code.behavioral.chain_of_responsibilty.ChainOfResponsibilityDemo
```
