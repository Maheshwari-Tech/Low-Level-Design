# Template Method Pattern

## Intent

Define an algorithm's invariant sequence in a base type while allowing subclasses to provide selected steps.

## When to use

- Several workflows share the same order and most of the same behavior.
- The sequence must remain controlled while individual steps vary.
- Duplicated workflow logic can be moved into one reusable base implementation.

## Participants and mechanics

- **Abstract class** owns the usually-final template method.
- **Primitive operations** are abstract or overridable steps implemented by subclasses.
- Optional **hooks** provide default no-op or common behavior.
- **Concrete classes** customize only designated steps; clients invoke the template method.

## Trade-offs

- Reuses workflow logic and protects its required ordering.
- Relies on inheritance and can become fragile when subclasses override too many hooks.
- Strategy is often preferable when behavior should be composed or changed at runtime.

## Implementation status

**Runnable.** [`PaymentFlow.java`](PaymentFlow.java) fixes validation, debit, transfer, and notification order in a final template method. [`PaymentDemo.java`](PaymentDemo.java) supplies card and wallet implementations for the variable steps.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/behavioral/template_method/*.java
java -cp "$out" code.behavioral.template_method.PaymentDemo
```
