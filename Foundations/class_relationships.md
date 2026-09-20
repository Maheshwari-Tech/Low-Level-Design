# Class Relationships

Choose a relationship by ownership, lifetime, and collaboration—not by diagram notation alone.

## Association

One object knows or collaborates with another. Neither object necessarily owns the other's lifecycle.

Example: a `Doctor` is assigned to several `Patient` records. The association may be one-to-one, one-to-many, or many-to-many and may be navigable in one or both directions.

## Aggregation

Aggregation is a weak whole-part association. The part can exist independently and may be reassigned.

Example: a `Team` aggregates `Player` objects. Removing the team does not erase each player's identity. In code, aggregation is usually represented by a reference or collection; the semantic lifecycle rule matters more than the hollow UML diamond.

## Composition

Composition is strong ownership. The whole creates or exclusively owns a part, and the part has no meaningful independent lifecycle.

Example: an `Order` composes `OrderLine` values. Deleting an unpersisted order discards its lines, and a line cannot move between orders without becoming a new line.

Prefer composition over inheritance when behavior can be delegated to a replaceable collaborator.

## Dependency

A dependency is temporary usage: a method parameter, local variable, returned value, or static call. The dependent does not retain the collaborator as part of its state.

Example: `Invoice.render(Formatter formatter)` depends on a formatter for one operation.

## Decision checklist

Ask:

1. Does the related object have identity outside the owner?
2. Who creates it and who controls its lifetime?
3. Can it be shared or moved safely?
4. Must the relationship be navigable both ways?
5. Does the model need a direct reference, or can an identifier/query avoid tight coupling?

Bidirectional relationships require two sides to stay consistent. Use them only when both navigation directions are genuine domain needs.
