# UML for Low-Level Design

UML is a communication tool, not the design itself. Use the smallest diagram that resolves a question, keep names aligned with code, and omit framework noise.

## Class diagram

Shows types, important fields/operations, inheritance, realization, multiplicity, and associations. Use it to communicate the stable object model and ownership boundaries.

Include only relationships that matter. Mark composition when lifetime is truly owned; do not infer it merely because one class has a field of another type.

## Use-case diagram

Shows actors and the goals they pursue through the system. It is useful at the start of an interview to establish scope and authorization boundaries, but it does not explain internal behavior.

## Sequence diagram

Shows messages over time for one workflow. Use it for order placement, a seat-hold race, payment callbacks, retries, or compensation. Include alternative/failure branches that drive the design.

## Activity diagram

Shows a workflow with decisions, parallel work, and joins. It is useful when a business process—such as fulfilment or approval—matters more than individual object messages.

## State-machine diagram

Shows valid states, events, guards, and transitions for a lifecycle such as `Payment`, `Order`, or `Reservation`. This is often the highest-value LLD diagram because it exposes illegal transitions and terminal states.

## Interview workflow

1. Capture actors and use cases.
2. Identify entities, value objects, services, and external ports in a class diagram.
3. Draw a state machine for each important lifecycle.
4. Walk one success and one failure path with a sequence diagram.
5. Update the class model when the behavioral diagrams reveal a missing responsibility.

## Quality checklist

- Every relationship has intentional direction and multiplicity.
- State-changing messages identify the owner of the invariant.
- External systems appear as boundaries, not as domain objects.
- Retries, timeouts, and concurrent conflicts appear where they change behavior.
- Diagram names and states match the README and implementation.

The imported [class-diagram archive](../Examples/awesome-low-level-design/class-diagrams/) contains the original upstream visual examples and retains its GPL-3.0 license and provenance.
