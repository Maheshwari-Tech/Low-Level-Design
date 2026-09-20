# Interface Segregation Principle

## Principle

Clients should not depend on operations they do not use. Prefer small, role-oriented interfaces over one broad interface that forces implementations to provide meaningless behavior.

## Apply it when

- Implementations contain empty methods, unsupported-operation errors, or irrelevant dependencies.
- Different clients use disjoint portions of the same interface.
- Changing one operation causes recompilation or retesting of unrelated clients.

## Mechanics

- Define interfaces around client roles and cohesive capabilities.
- Let implementations compose multiple capability interfaces when they legitimately support several roles.
- Accept the narrowest required interface at each collaboration boundary.

## Trade-offs

- Reduces coupling and makes contracts more honest and easier to implement or mock.
- Too many single-method interfaces can fragment a simple domain and complicate discovery.
- Split by meaningful client needs, not mechanically by method count.

## Implementation status

**Runnable.** [`BeforeViolation.java`](BeforeViolation.java) forces human and robot workers to implement the same broad `Worker` interface. [`WorkerDemo.java`](WorkerDemo.java) replaces it with focused capabilities such as `Workable`, `Codeable`, and `Cleanable`, then runs the corrected design.

## Run

The before and after files reuse worker type names, so compile them separately:

```bash
before="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$before" Principles/interface_segregation/BeforeViolation.java

after="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$after" Principles/interface_segregation/WorkerDemo.java
java -cp "$after" WorkerDemo
```
