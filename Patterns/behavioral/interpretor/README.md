# Interpreter Pattern

## Intent

Model a small grammar as an expression tree and evaluate sentences in that language against a context.

## When to use

- The grammar is small, stable, and naturally represented as recursive expressions.
- Rules or expressions must be composed at runtime.
- A full parser generator or query engine would be disproportionate to the problem.

## Participants and mechanics

- **Expression** defines an `interpret(context)` operation.
- **Terminal expressions** evaluate atomic grammar symbols.
- **Nonterminal expressions** combine child expressions according to grammar rules.
- **Context** provides input and shared evaluation state.
- The client parses or builds an expression tree, then evaluates its root.

## Trade-offs

- Makes simple grammar rules explicit and easy to extend with new expression types.
- Produces many classes and can be slow or unwieldy for complex grammars.
- Parsing, validation, recursion depth, and untrusted input need separate safeguards.

## Implementation status

**Runnable.** [`InterpreterDemo.java`](InterpreterDemo.java) models a small Boolean grammar with terminal and nonterminal expression records. The historical folder name `interpretor` is retained for path compatibility.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/interpretor/InterpreterDemo.java
java -cp "$out" code.behavioral.interpretor.InterpreterDemo
```
