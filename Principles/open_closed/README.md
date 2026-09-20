# Open/Closed Principle

## Principle

Software units should be open to expected extension but closed to repeated modification of stable behavior. New variants should plug into an established contract instead of expanding type checks in mature policy code.

## Apply it when

- Adding each new variant requires editing the same conditional or switch.
- Stable business policy is coupled to volatile concrete types.
- The variation axis is understood well enough to define a meaningful contract.

## Mechanics

- Isolate what varies behind an interface or another explicit extension point.
- Move variant-specific behavior into implementations of that contract.
- Make stable clients operate on the abstraction, and keep implementation selection at a composition boundary or factory.

## Trade-offs

- Reduces risk to stable code and supports focused testing of new variants.
- Predicting every extension point creates speculative abstractions and unnecessary complexity.
- Existing contracts still need revision when requirements change along an unanticipated axis.

## Implementation status

**Runnable.** [`AreaCalculator.java`](AreaCalculator.java) shows the violation: the calculator checks concrete shape types and must change for every new shape. [`Shape.java`](Shape.java) moves area calculation behind a `Shape` contract, adds `Triangle` without changing the calculator, and contains the runnable fixed demonstration.

## Run

The before and after files both define `AreaCalculator`, so compile them separately:

```bash
before="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$before" Principles/open_closed/AreaCalculator.java

after="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$after" Principles/open_closed/Shape.java
java -cp "$after" AreaCalculator
```
