# Factory Pattern

## Intent

Move object-creation decisions behind a named operation so clients depend on a product abstraction instead of concrete constructors.

## When to use

- The concrete product depends on input, configuration, or runtime context.
- Creation includes validation, caching, pooling, or other policy.
- Client code should work with a stable product interface as new variants are added.

## Participants and mechanics

- **Product** defines the interface used by clients.
- **Concrete products** provide alternative implementations.
- A **factory** chooses and creates the appropriate product. In the GoF Factory Method variant, subclasses override that creation method; a simple factory keeps the choice in one factory object.
- The client asks the factory for a product and then uses only the product contract.

## Trade-offs

- Concentrates construction policy and reduces direct coupling to concrete classes.
- Supports additional product implementations with limited client changes.
- Can become a large conditional factory; Factory Method also introduces a creator hierarchy.

## Implementation status

**Runnable.** [`FactoryMethodDemo.java`](FactoryMethodDemo.java) is the GoF Factory Method example: concrete dialog creators override product construction. The earlier [`ShapeFactory.java`](ShapeFactory.java) remains as a distinct simple-factory variant that returns [`Circle`](Circle.java) or [`Rectangle`](Rectangle.java) through [`Shape`](Shape.java). Additional notes are in [`factory.md`](factory.md).

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/creational/factory/*.java
java -cp "$out" code.creational.factory.FactoryMethodDemo
```
