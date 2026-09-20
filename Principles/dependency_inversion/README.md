# Dependency Inversion Principle

## Principle

High-level policy should not depend directly on low-level infrastructure; both should depend on abstractions owned around the policy's needs. Details implement those abstractions rather than dictating the design of the policy.

## Apply it when

- Business logic constructs database, network, filesystem, or vendor clients directly.
- Replacing infrastructure requires editing high-level workflows.
- Unit tests cannot exercise policy without starting real external resources.

## Mechanics

- Define a narrow port in terms of the high-level module's required behavior.
- Implement that port in low-level adapters.
- Supply the implementation from a composition root, commonly through constructor injection.
- Dependency injection is a wiring technique; dependency inversion is the direction of source-code dependency.

## Trade-offs

- Keeps policy independent, makes implementations replaceable, and enables lightweight test doubles.
- Adds interfaces and wiring that may not pay off for stable, trivial details.
- Poorly designed abstractions can merely mirror a vendor API and fail to invert ownership meaningfully.

## Implementation status

**Runnable.** [`BeforeViolation.java`](BeforeViolation.java) has `UserService` construct `MySQLDatabase` directly. [`DatabaseDemo.java`](DatabaseDemo.java) introduces a policy-facing `Database` contract, injects MySQL or PostgreSQL implementations, and runs both choices.

## Run

The before and after files reuse infrastructure type names, so compile them separately:

```bash
before="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$before" Principles/dependency_inversion/BeforeViolation.java

after="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$after" Principles/dependency_inversion/DatabaseDemo.java
java -cp "$after" DatabaseDemo
```
