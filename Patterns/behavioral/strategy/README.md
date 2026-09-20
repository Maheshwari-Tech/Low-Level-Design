# Strategy Pattern

## Intent

Encapsulate interchangeable algorithms behind one contract and let a context select or receive the appropriate algorithm.

## When to use

- Several algorithms solve the same problem with different policies or trade-offs.
- Conditional branches select behavior by type, configuration, or runtime context.
- Algorithms should be tested and changed independently of the object using them.

## Participants and mechanics

- **Strategy** defines the algorithm contract.
- **Concrete strategies** implement alternative algorithms.
- **Context** delegates the varying operation to a strategy, usually supplied through construction or configuration.
- **Client** chooses a strategy based on the use case; the context remains unaware of concrete strategy details.

## Trade-offs

- Favors composition, removes duplicated behavior, and supports runtime selection.
- Adds types and moves selection responsibility to a client or factory.
- Strategies may need context data, which should be passed explicitly without exposing unrelated internals.

## Implementation status

**Runnable.** The [`vehicleExample/good`](vehicleExample/good/) version injects an [`IDriveStrategy`](vehicleExample/good/IDriveStrategy.java) and shares `SpecialDrive` across vehicle types. [`Main.java`](vehicleExample/good/Main.java) runs all variants. [`vehicleExample/bad`](vehicleExample/bad/) shows the duplicated inheritance-based behavior it replaces; supporting notes are in [`strategy.md`](strategy.md).

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/strategy/vehicleExample/good/*.java
java -cp "$out" code.behavioral.strategy.vehicleExample.good.Main
```
