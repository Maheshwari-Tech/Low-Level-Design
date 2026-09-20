# Command Pattern

## Intent

Represent a request as an object so it can be queued, logged, composed, retried, or undone independently of the caller.

## When to use

- A caller should trigger work without knowing the receiver or operation details.
- Operations need scheduling, persistence, retry, audit, or undo/redo.
- The same operation should be invoked from different UI, API, or messaging entry points.

## Participants and mechanics

- **Command** defines `execute` and, when applicable, `undo`.
- **Concrete command** stores the receiver and parameters needed for one request.
- **Receiver** performs the domain work.
- **Invoker** triggers or stores commands without knowing their implementation.
- **Client** wires commands to receivers and supplies them to the invoker.

## Trade-offs

- Decouples invocation from execution and turns operations into composable data.
- Adds command classes and requires careful capture of mutable parameters.
- Reliable undo may require snapshots or compensating actions and is not always possible.

## Implementation status

**Runnable.** [`CommandDemo.java`](CommandDemo.java) encapsulates editor mutations as commands, records them in an invoker, and undoes the most recent command.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/command/CommandDemo.java
java -cp "$out" code.behavioral.command.CommandDemo
```
