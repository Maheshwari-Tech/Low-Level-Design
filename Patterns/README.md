# Design Patterns

This directory is the authoritative index for the 23 Gang of Four design patterns. Each linked page explains the pattern's intent, appropriate use, mechanics, trade-offs, and any example available in this repository.

## Creational patterns

Creational patterns separate object construction from the code that uses the objects.

| Pattern | Purpose |
| --- | --- |
| [Abstract Factory](creational/abstract_factory/) | Create compatible families of related objects. |
| [Builder](creational/builder/) | Assemble a complex object step by step. |
| [Factory](creational/factory/) | Centralize or defer the choice of concrete product. |
| [Prototype](creational/prototype/) | Create objects by copying configured prototypes. |
| [Singleton](creational/singleton/) | Provide one controlled instance of a class. |

## Structural patterns

Structural patterns compose classes and objects into larger structures while keeping dependencies manageable.

| Pattern | Purpose |
| --- | --- |
| [Adapter](structural/adaptor/) | Translate one interface into another expected by a client. |
| [Bridge](structural/bridge/) | Vary an abstraction and its implementation independently. |
| [Composite](structural/composite/) | Treat individual objects and object trees uniformly. |
| [Decorator](structural/decorator/) | Add responsibilities to an object through composition. |
| [Facade](structural/facade/) | Expose a simpler entry point to a complex subsystem. |
| [Flyweight](structural/flyweight/) | Share reusable intrinsic state across many logical objects. |
| [Proxy](structural/proxy/) | Control access to another object through the same interface. |

## Behavioral patterns

Behavioral patterns organize communication, responsibility, and algorithms among objects.

| Pattern | Purpose |
| --- | --- |
| [Chain of Responsibility](behavioral/chain_of_responsibilty/) | Offer a request to an ordered sequence of handlers. |
| [Command](behavioral/command/) | Represent an operation as an object. |
| [Interpreter](behavioral/interpretor/) | Evaluate sentences in a small language or grammar. |
| [Iterator](behavioral/iterator/) | Traverse a collection without exposing its representation. |
| [Mediator](behavioral/mediator/) | Centralize interactions among collaborating objects. |
| [Memento](behavioral/momento/) | Capture and restore an object's state without exposing internals. |
| [Observer](behavioral/observer/) | Notify dependents when a subject changes. |
| [State](behavioral/state/) | Change an object's behavior when its state changes. |
| [Strategy](behavioral/strategy/) | Select an interchangeable algorithm at runtime. |
| [Template Method](behavioral/template_method/) | Fix an algorithm's outline while allowing selected steps to vary. |
| [Visitor](behavioral/visitor/) | Add operations to a stable object structure. |

## Repository conventions

- Existing folder names are retained to avoid breaking paths; documentation uses the canonical names **Adapter**, **Chain of Responsibility**, **Interpreter**, and **Memento**.
- Coverage is **23/23 runnable GoF examples**. Each pattern page identifies its canonical entry point and focused run command.
- Examples are intentionally compact and in-memory so the pattern remains visible; they are not production service implementations.
- Start with the problem and trade-offs on a pattern page; use a pattern only when those forces are present.

## Compile all examples

From the repository root, compile every pattern with the Java 17 language/API surface and all lint checks enabled:

```bash
out="$(mktemp -d)"
find Patterns -name '*.java' -print0 | xargs -0 javac --release 17 -Xlint:all -d "$out"
```

Use the `Run` command on an individual pattern page to execute its canonical demonstration.

For additional implementations across six languages, see the licensed and attributed [upstream pattern archive](../Examples/awesome-low-level-design/design-patterns/).
