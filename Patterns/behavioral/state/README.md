# State Pattern

## Intent

Represent state-dependent behavior with interchangeable state objects so a context changes behavior when its state changes.

## When to use

- Operations contain repeated conditionals based on a lifecycle state.
- Each state permits different actions or transition rules.
- New states should be added without expanding conditionals throughout the context.

## Participants and mechanics

- **Context** stores the current state and delegates state-dependent operations.
- **State** defines the operations available in every state.
- **Concrete states** implement behavior and may request a transition.
- Transitions may be owned by the context, the state objects, or a separate transition table; choose one policy consistently.

## Trade-offs

- Localizes behavior and transitions for each state and removes large conditional blocks.
- Adds classes and can scatter transition logic if ownership is unclear.
- A simple enum and switch may be clearer for small, stable state machines.

## Implementation status

**Runnable.** [`StateDemo.java`](StateDemo.java) models red, green, and amber as state objects that own their transition behavior.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/state/StateDemo.java
java -cp "$out" code.behavioral.state.StateDemo
```
