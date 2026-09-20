# Extensible Calculator

## Interview Prompt

Design a calculator that evaluates operations, validates operands, records history, and can add a new operation without editing the dispatcher. A command-oriented variation should also support undo where the operation is reversible.

## Requirements and Model

- Register operations by stable symbol/name and reject duplicate registrations.
- Evaluate exact decimal values with an explicit `MathContext` and division-by-zero policy.
- Separate parsing from evaluation so the same engine supports a CLI, UI, or API.
- Record immutable calculation history; a retried command must not create two entries.
- Support unary and binary operations without a type switch in the core engine.

| Type | Responsibility |
| --- | --- |
| `Operation` | Strategy that declares arity and evaluates operands |
| `OperationRegistry` | Maps a token to one operation implementation |
| `CalculateCommand` | Operation token, operands, actor, and idempotency key |
| `Calculator` | Validates arity, invokes policy, and records the result |
| `Calculation` | Immutable expression/result/history entry |
| `UndoableCommand` | Optional command contract with captured prior state |

## Invariants and API

- The number of operands equals the selected operation's arity.
- Evaluation never mutates the caller's operand collection.
- Division, overflow, domain, and parse errors are explicit results or typed exceptions.
- History order is stable and entries are append-only.

```java
void register(String token, Operation operation);
Calculation calculate(CalculateCommand command);
Optional<Calculation> findByIdempotencyKey(String key);
List<Calculation> history(int limit);
```

## Design Solution

Use Strategy for mathematical behavior and a registry/factory for lookup. A command object is valuable when history, queuing, macro composition, or undo is required; it is unnecessary for a pure one-shot arithmetic function. Keep expression tokenization in a separate parser and convert the expression into an AST or postfix sequence before invoking operations. Persist the idempotency key and history entry atomically.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/Calculator/` — Java command-pattern implementation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Calculator)
2. `References/kumaransg-LLD/Low_level_Design_Problems/Calculator-Alternative/` — a second Java command/factory design; preserve it as an alternate approach. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Calculator-Alternative)

## Follow-Ups

Operator precedence and parentheses, variables, scientific functions, arbitrary precision, command macros, and concurrent histories.

## Implementation Status

The maintained page provides the complete interview design. Both actual Java approaches remain unmodified in the local source clone.
