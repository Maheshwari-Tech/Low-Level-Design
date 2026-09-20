# Foundations

Use these notes before the pattern and problem catalog. They collect the foundational material from the source repositories into one Java-oriented learning path.

## Learning path

1. [60-minute Senior/Staff LLD interview playbook](senior_staff_lld_interview.md): framing, answer structure, SOLID and pattern reasoning, concurrency, follow-ups, and scoring.
2. [Object-oriented programming](object_oriented_programming.md): objects, classes, interfaces, enums, encapsulation, abstraction, inheritance, polymorphism, immutability, and access control.
3. [Class relationships](class_relationships.md): association, aggregation, composition, and dependency.
4. [Clean code](clean_code.md): naming, cohesive modules, abstraction levels, and pragmatic simplicity.
5. [Design principles](../Principles/): SOLID, DRY, KISS, YAGNI, and Law of Demeter.
6. [UML](../UML/): communicating a design with structural and behavioral diagrams.
7. [Testing and testability](testing.md): unit tests, assertions, test doubles, dependency injection, and design feedback.
8. [Design patterns](../Patterns/): reusable object collaboration patterns.
9. [Concurrency](../Concurrency/): safe shared state and coordination.

## Recommended study loop

For each concept, write the smallest runnable example, identify its invariant, add a failing test for an edge case, and then explain why the chosen abstraction is better than a simpler alternative. Apply patterns only when the problem creates the pressure for them.

The attributed [multi-language OOP archive](../Examples/awesome-low-level-design/oop/) provides additional examples in C++, C#, Go, Java, Python, and Rust.
