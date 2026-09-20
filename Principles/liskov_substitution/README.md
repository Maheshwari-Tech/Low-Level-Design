# Liskov Substitution Principle

## Principle

Every subtype must be usable wherever its base type is expected without surprising the client or breaking the base contract. Subtyping is a behavioral promise, not only a shared method signature.

## Apply it when

- An override rejects valid base inputs, changes postconditions, or throws new unexpected errors.
- Client code checks a subtype before invoking base behavior.
- A subclass disables inherited operations or changes invariants that callers rely on.

## Mechanics

- State the base type's preconditions, postconditions, invariants, and error behavior.
- Do not strengthen preconditions or weaken postconditions in a subtype.
- Model concepts with separate interfaces or composition when they do not share the same behavioral contract.

## Trade-offs

- Preserves reliable polymorphism and keeps callers free of subtype-specific branches.
- May require replacing a convenient inheritance hierarchy with several smaller abstractions.
- A mathematically valid “is-a” relationship does not guarantee behavioral substitutability in a mutable API.

## Implementation status

**Runnable.** [`AreaCalculator.java`](AreaCalculator.java) demonstrates the mutable Rectangle/Square violation: setting width and height through the base type produces different semantics for `Square`. [`Shape.java`](Shape.java) gives rectangle and square a substitutable area contract while keeping their mutations separate. Both files include executable comparisons.

## Run

The before and after files reuse type names, so compile and run them separately:

```bash
before="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$before" Principles/liskov_substitution/AreaCalculator.java
java -cp "$before" AreaCalculator

after="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$after" Principles/liskov_substitution/Shape.java
java -cp "$after" AreaCalculator
```
