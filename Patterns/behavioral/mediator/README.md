# Mediator Pattern

## Intent

Encapsulate how a group of objects collaborate so they depend on a mediator rather than directly on one another.

## When to use

- Many peer objects have tangled, many-to-many communication.
- Interaction rules should change independently of participant classes.
- A workflow coordinator can express the collaboration more clearly than distributed callbacks.

## Participants and mechanics

- **Mediator** defines operations used by participants to communicate.
- **Concrete mediator** holds participant references and applies coordination rules.
- **Colleagues** know the mediator and send events or requests through it.
- The mediator invokes or notifies the appropriate colleagues; colleagues do not address one another directly.

## Trade-offs

- Reduces coupling among colleagues and centralizes interaction policy.
- The mediator can grow into a complex god object if it absorbs participant-specific business logic.
- Central coordination may become a performance or availability bottleneck.

## Implementation status

**Runnable.** The [`OnlineAuctionSystem`](OnlineAuctionSystem/) example uses [`Auction`](OnlineAuctionSystem/Auction.java) as the mediator for bidders. [`Main.java`](OnlineAuctionSystem/Main.java) registers bidders and submits bids through the mediator.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/mediator/OnlineAuctionSystem/*.java
java -cp "$out" code.behavioral.mediator.OnlineAuctionSystem.Main
```
