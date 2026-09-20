# Memento Pattern

## Intent

Capture an object's internal state so it can be restored later without exposing that state to the object managing history.

## When to use

- A feature needs undo, checkpoints, rollback, or version history.
- Only the originating object should interpret the saved state.
- Reconstructing an earlier state from events would be more complex than storing snapshots.

## Participants and mechanics

- **Originator** creates a memento from its current state and can restore one.
- **Memento** stores the snapshot while restricting access to its representation.
- **Caretaker** stores and selects mementos but does not inspect or mutate their contents.
- Snapshot frequency, retention, and copy depth define the memory and correctness policy.

## Trade-offs

- Preserves encapsulation while enabling straightforward rollback.
- Snapshots can consume substantial memory and may retain obsolete object graphs.
- Mutable or external resources require deep-copy or reconstruction rules.

## Implementation status

**Runnable.** [`ConfigurationOriginator.java`](ConfigurationOriginator.java) creates immutable configuration snapshots, [`ConfigurationCareTaker.java`](ConfigurationCareTaker.java) stores history, and [`Demo.java`](Demo.java) restores the previous state. Code identifiers and the folder retain the historical `Momento` spelling for path compatibility; the pattern's canonical name is **Memento**.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/momento/*.java
java -cp "$out" code.behavioral.momento.Demo
```
