# Flyweight Pattern

## Intent

Reduce memory use by sharing immutable intrinsic state among many logical objects while supplying context-specific extrinsic state at operation time.

## When to use

- The application creates a very large number of similar fine-grained objects.
- Much of each object's state is repeated and can be made immutable.
- Object identity is less important than value and behavior.

## Participants and mechanics

- **Flyweight** defines operations that accept extrinsic state.
- **Concrete flyweight** stores reusable intrinsic state.
- **Flyweight factory** returns a cached instance for an intrinsic-state key.
- **Client** owns or computes extrinsic state and passes it to the flyweight for each use.

## Trade-offs

- Can greatly reduce allocation and memory pressure.
- Adds lookup and state-splitting complexity; shared state must not be mutated per client.
- The benefit is small when instances are few or their state is mostly unique.

## Implementation status

**Runnable.** The [`Game`](Game/) example shares robot type and sprite data while passing coordinates to `display`. The [`wordProcessor`](wordProcessor/) example caches character/font/size combinations and supplies row and column as extrinsic state. Both include executable demonstrations.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/flyweight/Game/{bad,good}/*.java Patterns/structural/flyweight/wordProcessor/{bad,good}/*.java
java -cp "$out" code.structural.flyweight.Game.good.Main
java -cp "$out" code.structural.flyweight.wordProcessor.good.Main
```
