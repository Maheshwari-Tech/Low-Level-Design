# Machine Coding Round: What Is Actually Expected

## Typical format

- 10–15 minutes: clarify requirements, scope, inputs, outputs, and edge cases.
- 60–90 minutes: produce executable, functionally complete code.
- 15–30 minutes: demo with test cases, explain choices, and respond to an extension request.

The [workattech Flipkart preparation guide](https://github.com/workattech/flipkart-machine-coding-round) describes the same pre-coding, coding, and post-coding stages and explicitly calls out readability, modularity, testability, correctness, completeness, and error handling. A [documented Flipkart SDE-II round](https://www.geeksforgeeks.org/interview-experiences/flipkart-machine-coding-round-experience/) additionally specifies in-memory data, a runnable driver/tests, proper abstraction and modeling, and accommodating new requirements with minimal changes.

## Evaluation rubric used for these solutions

| Area | What the reviewer should see |
|---|---|
| Completeness | Required use cases run end to end; optional features are clearly separated. |
| Correctness | Domain invariants and negative cases are tested. |
| Modeling | Names reflect the domain; invalid state is rejected close to the model. |
| Separation | Domain has no FastAPI/React dependency; HTTP and persistence are adapters. |
| Extensibility | New repository, strategy, or policy can be added without rewriting use cases. |
| Code hygiene | Small cohesive modules, type hints, explicit errors, no leaked mutable collections. |
| Demonstrability | One command runs the backend; API docs and a small UI exercise the flow. |

## What not to optimize for

- Production-scale infrastructure in a 90-minute round.
- Design patterns without a concrete variation point.
- A polished UI at the cost of domain behavior and tests.
- Premature distributed-system complexity. State the production race conditions and solution, but first deliver the scoped in-memory program.

## Recommended interview sequence

1. Restate the functional requirements and explicitly mark out-of-scope behavior.
2. Identify aggregates, entities, value objects, and invariants.
3. Define use cases and repository contracts.
4. Implement the happy path vertically and keep it runnable.
5. Add validation, negative cases, and focused unit tests.
6. Demo required examples, then discuss concurrency and production evolution.

## Staff-role 90-minute plan

| Time | Deliverable | Staff-level signal |
|---:|---|---|
| 0–10 min | Clarified scope, invariants, failure cases | Separates must-have behavior from production follow-ups |
| 10–20 min | Aggregate, value objects, use cases, consistency boundary | Explains ownership instead of drawing a class zoo |
| 20–55 min | One runnable vertical slice, then remaining commands | Keeps the system executable while building |
| 55–70 min | Negative, boundary, and race-oriented tests | Tests risks, not getters |
| 70–80 min | Refactor names and dependency direction | Removes accidental coupling without pattern inflation |
| 80–90 min | Demo, trade-offs, scale path, next extension | Communicates judgment and leaves a credible roadmap |

### Narration expected from a staff candidate

1. State the consistency boundary before discussing classes.
2. Name the invariant that each atomic operation protects.
3. Separate process-local thread safety from multi-instance correctness.
4. Explain where idempotency, database constraints, and an outbox would enter.
5. Quantify the current complexity and the signal that would trigger optimization.
6. Say which pattern was intentionally not introduced and why.

### Extension drill

Reserve the final ten minutes for one change. A good design should absorb it near an existing seam:

- Battleship: multiple game IDs or a different fleet configuration.
- Movie booking: seat categories or idempotent confirmation.
- Calendar: office-specific hours or meeting rooms.

If the extension requires edits across every layer, explain whether that is inherent to the feature or evidence of a missing boundary.
