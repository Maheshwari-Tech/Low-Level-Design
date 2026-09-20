# Abstract Factory Pattern

## Intent

Create families of related products through common interfaces without making client code depend on their concrete classes.

## When to use

- The application must switch between product families, such as platform-specific UI widgets or vehicle tiers.
- Products from one family must be used together consistently.
- Construction details should stay outside the business workflow.

## Participants and mechanics

- **Abstract factory** declares one creation operation per product type.
- **Concrete factories** build one compatible product family.
- **Abstract products** define the contracts consumed by the client.
- **Concrete products** implement a contract for a particular family.
- The client receives a factory, requests products from it, and uses only product abstractions.

## Trade-offs

- Keeps family selection in one place and prevents incompatible combinations.
- Makes an entire family easy to replace or test.
- Adds interfaces and classes, and adding a new product type requires changing every factory contract.

## Implementation status

**Runnable.** [`VehicleFactory.java`](VehicleFactory.java) creates a compatible `Vehicle` and [`MaintenancePlan`](MaintenancePlan.java) family. `OrdinaryFactory` and the historically named `LuxaryFactory` supply two families; [`AbstractFactoryDemo.java`](AbstractFactoryDemo.java) selects both through the factory abstraction.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/creational/abstract_factory/*.java
java -cp "$out" code.creational.abstract_factory.AbstractFactoryDemo
```
