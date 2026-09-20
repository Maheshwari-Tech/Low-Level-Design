# Observer Pattern

## Intent

Define a one-to-many dependency so registered observers are notified when a subject's state or event stream changes.

## When to use

- Several independent consumers react to the same domain event.
- Publishers should not know concrete subscriber types.
- Subscribers must be added or removed at runtime.

## Participants and mechanics

- **Subject** registers, removes, and notifies observers.
- **Observer** defines the update contract.
- **Concrete subject** owns state or emits events.
- **Concrete observers** react to pushed event data or pull current state from the subject.
- The contract must define ordering, unsubscribe behavior, duplicate registration, re-entrancy, and synchronous versus asynchronous delivery.

## Trade-offs

- Decouples publishers from subscribers and supports dynamic fan-out.
- Notification chains can be difficult to trace; slow or failing observers can affect synchronous publishers.
- Delivery guarantees, backpressure, and thread safety require explicit policies in production systems.

## Implementation status

**Runnable.** [`IphoneExample/good`](IphoneExample/good/) implements a stock subject with email and SMS observers. [`Main.java`](IphoneExample/good/Main.java) demonstrates subscription, notification on restock, and unsubscription. Earlier comparison notes are in [`observer.md`](observer.md).

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/observer/IphoneExample/good/*.java
java -cp "$out" code.behavioral.observer.IphoneExample.good.Main
```
