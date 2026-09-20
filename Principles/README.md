# SOLID Design Principles

SOLID is a set of design heuristics for keeping object-oriented code cohesive, extensible, substitutable, focused, and loosely coupled. Apply the principles in response to concrete change and testing pressure; they are not goals for maximizing the number of interfaces or classes.

| Principle | Design question | Local guide |
| --- | --- | --- |
| **S**ingle Responsibility | Does this unit have one coherent reason to change? | [Single Responsibility Principle](single_responsibility/) |
| **O**pen/Closed | Can expected variants be added without editing stable policy? | [Open/Closed Principle](open_closed/) |
| **L**iskov Substitution | Can every subtype honor the observable contract of its base type? | [Liskov Substitution Principle](liskov_substitution/) |
| **I**nterface Segregation | Does each client depend only on operations it needs? | [Interface Segregation Principle](interface_segregation/) |
| **D**ependency Inversion | Does policy depend on abstractions rather than infrastructure details? | [Dependency Inversion Principle](dependency_inversion/) |

## How to use this section

Each principle page states the design pressure it addresses, the mechanics of applying it, its trade-offs, and the local before/after example where one exists. The examples are intentionally small so the effect of one principle remains visible.

Complementary guidance such as DRY, KISS, YAGNI, and the Law of Demeter is summarized in [`other.md`](other.md). [`solid.md`](solid.md) contains the repository's original working notes; the linked principle pages above are the maintained reference.

## Implementation status

Coverage is **5/5 runnable SOLID examples**. Each principle page provides Java 17 compile and run commands for its focused demonstration.

The historical before/after files intentionally reuse simple class names in the default package. Compile those alternatives into separate output directories as shown on their pages; compiling every file under `Principles/` in one `javac` invocation will produce duplicate-class errors.
