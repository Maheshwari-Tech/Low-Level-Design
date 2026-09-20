# Object-Oriented Programming

Object-oriented design groups state and the operations that protect that state behind explicit boundaries. The goal is not to maximize the number of classes; it is to create objects with clear responsibilities and valid behavior.

## Core building blocks

### Classes and objects

A class defines a type's state, construction rules, and behavior. An object is one instance of that type. Constructors should establish a valid object; avoid instances that require a sequence of setters before they can be used.

```java
public record Money(long minorUnits, Currency currency) {
    public Money {
        if (minorUnits < 0) throw new IllegalArgumentException("negative amount");
        Objects.requireNonNull(currency);
    }
}
```

### Enums

Use an enum for a closed, meaningful set of values such as `OrderStatus`. Give it behavior when the behavior belongs to the value. Do not use an enum when callers must add new variants independently.

### Interfaces

An interface is a client-facing contract. Prefer small, role-specific interfaces that describe capability. An interface is valuable when it enables multiple implementations, isolates an external dependency, or defines a stable collaboration seam—not merely to prefix a concrete class with `I`.

## The four pillars

### Encapsulation

Keep representation private and expose operations that preserve invariants. Returning a mutable internal collection or allowing arbitrary state setters breaks encapsulation even when fields are private.

### Abstraction

Expose what a collaborator needs and hide how it is achieved. `PaymentGateway.authorize(request)` is an abstraction; leaking provider-specific response objects through the domain is not.

### Inheritance

Inheritance expresses an **is-a** relationship and a behavioral contract. Use it only when every subtype can replace the base type. Prefer composition when the goal is code reuse or configurable behavior.

### Polymorphism

Polymorphism lets the caller use one contract while implementations vary. Strategy, adapter, and state objects are common forms. Java generics provide parametric polymorphism when an algorithm should work safely over several types.

## Java design tools

- Use `final`, records, defensive copies, and unmodifiable views for immutable value objects.
- Use `static` for behavior or data that belongs to the type, not as a global-state shortcut.
- Choose the narrowest access: `private`, package-private, `protected`, then `public`.
- Make identity entities explicit; do not confuse them with value objects whose equality is based on contents.
- Validate required input at the boundary and enforce domain invariants inside the owning object.

## Common traps

- An anemic model containing only getters and setters while a single service owns every rule.
- Deep inheritance trees that require type checks or unsupported overrides.
- Public mutable collections and shared global state.
- One interface per class without an actual abstraction boundary.
- A “manager” class with unrelated responsibilities and no cohesive invariant.
