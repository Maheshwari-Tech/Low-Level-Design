# LLD Interview Playbook

This folder is guidance, not a separate system-design problem.

## A practical flow

1. Restate the goal, actors, and the first version's boundary.
2. Write functional requirements and explicitly defer unrelated features.
3. Identify invariants, lifecycle states, time-based behavior, and competing commands.
4. Model value objects, entities, policies, repositories, domain services, and external ports.
5. Sketch the class/state diagrams and walk one success sequence.
6. Walk invalid, failure, retry, expiry, and concurrency sequences.
7. Define APIs from use cases, then write the smallest coherent code slice.
8. State trade-offs and extensions after the core design is correct.

## What interviewers look for

- Requirements drive the abstractions; patterns do not drive requirements.
- The object that owns an invariant also controls its mutation.
- State transitions are explicit and invalid transitions fail clearly.
- External providers and persistence are behind narrow interfaces.
- Time, IDs, randomness, and policies are controllable in tests.
- Concurrency discussion names the shared state and atomic decision.
- The design remains readable without speculative layers.

## Deliverables

A strong answer includes a requirement list, core model, important APIs, one state machine, one success and one failure/concurrency flow, design choices, and tests or a demo. Use the repository [Foundations](../../Foundations/), [UML guide](../../UML/), and [problem catalog](../) as the supporting checklist.

The existing Java `Main` is only a placeholder and is not presented as a complete implementation.
