# Clean Code for LLD

## Naming

- Name types as domain nouns and operations as commands or queries: `Reservation`, `reserve`, `findAvailable`.
- Include units and semantics: `timeoutMillis`, `priceInMinorUnits`, `expiresAt`.
- Avoid vague containers such as `Data`, `Util`, `Helper`, or `Manager` unless the responsibility is genuinely clear.
- Use one term consistently; do not alternate between `customer`, `user`, and `buyer` for the same role.

## Functions and modules

- Keep one level of abstraction in a method.
- Separate state-changing commands from side-effect-free queries where practical.
- Put a business rule next to the state it protects.
- Extract a policy when behavior varies; do not create an abstraction for code that has only one simple path.
- Keep I/O, frameworks, and vendor SDKs at adapters around the domain.

## Error handling

Reject invalid input early with domain-specific errors. Do not use `null`, magic values, or swallowed exceptions as control flow. Preserve enough context to diagnose a failure without exposing credentials or personal data.

## Pragmatic principles

The repository's [supporting principles](../Principles/other.md) cover DRY, KISS, YAGNI, and the Law of Demeter. Apply them together:

- Remove duplicated **knowledge**, not every similar-looking line.
- Prefer the smallest design that preserves known invariants.
- Delay hypothetical extension points until a real variation is visible.
- Ask an object to perform an operation instead of navigating through its internals.

## Review checklist

- Can each class state its responsibility in one sentence?
- Does every public method preserve an explicit invariant?
- Are time, randomness, IDs, and external systems injectable where tests need control?
- Are state transitions visible and validated?
- Can a new policy be added without editing unrelated domain objects?
- Is a simpler design sufficient?
