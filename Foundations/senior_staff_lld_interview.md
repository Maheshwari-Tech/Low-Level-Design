# 60-Minute Senior/Staff LLD Interview Playbook

Use this guide to turn an open-ended design prompt into a focused, defensible
low-level design within one hour. The goal is not to draw the most classes. The goal
is to discover the important invariants, place behavior behind clear ownership
boundaries, and show how the design behaves when requests race or dependencies fail.

## What the interviewer is evaluating

| Signal | What a strong answer demonstrates |
| --- | --- |
| Problem framing | Clarifies actors, use cases, scale, consistency, and exclusions before modeling |
| Domain judgment | Finds the few rules that must always remain true and gives each rule an owner |
| Object design | Uses cohesive entities, value objects, policies, services, and ports instead of data bags |
| Changeability | Identifies likely variation points without abstracting every class prematurely |
| Correctness | Defines legal state transitions, transaction boundaries, idempotency, and concurrency control |
| Failure reasoning | Handles timeouts, partial success, retries, duplicate events, and unknown outcomes explicitly |
| Communication | Keeps one coherent design narrative and can defend alternatives and trade-offs |

Senior candidates are expected to produce a correct, testable design for the agreed
scope. Staff candidates should additionally expose ownership boundaries, evolutionary
paths, operational consequences, and safe migration choices without turning the LLD
round into a generic distributed-systems lecture.

## The 60-minute answer plan

| Time | Candidate output | Interviewer signal |
| ---: | --- | --- |
| 0–5 min | Restate the problem, identify actors, ask the highest-value questions | Controls ambiguity instead of silently assuming |
| 5–10 min | Commit to must-have use cases, non-functional needs, and exclusions | Can bound a one-hour solution |
| 10–18 min | Write invariants and the lifecycle/state machine | Designs around correctness, not nouns |
| 18–30 min | Draw the domain model and command/query APIs | Places behavior and dependencies deliberately |
| 30–40 min | Walk one success path and one failure path | Makes object collaboration concrete |
| 40–50 min | Resolve races, retries, idempotency, time, and transaction boundaries | Understands production failure semantics |
| 50–56 min | Explain SOLID choices, justified patterns, and rejected alternatives | Uses principles as reasoning tools |
| 56–60 min | Summarize trade-offs, tests, observability, and the next evolution step | Finishes with a coherent design contract |

Do not spend the first 20 minutes drawing every possible class. Write three to seven
invariants early; they determine which objects and synchronization boundaries matter.

## Questions to ask before drawing classes

Ask only questions that change the design. State a reasonable assumption when the
interviewer has no preference.

### Scope and actors

1. **Who issues commands, and who consumes results or events?**
   Separate administrator, end-user, worker, and external-provider capabilities.
2. **Which use cases must be demonstrated in this hour?**
   Choose a narrow vertical slice plus one important failure path.
3. **What is explicitly out of scope?**
   UI, authentication, reporting, search relevance, and cross-region replication are
   common exclusions unless they change the object model.

### Correctness and consistency

1. **Which operation must be atomic?**
   Name the state protected by that boundary, not merely “use a transaction.”
2. **Can the caller retry? What identifies the same logical command?**
   Require an idempotency key and define what happens if the key is reused with
   different input.
3. **May reads be stale?**
   Strong command consistency and eventually consistent projections are often a good
   split, but it must be stated.
4. **What happens when a dependency times out after receiving the request?**
   Treat the outcome as unknown and reconcile; do not assume timeout means failure.

### Scale, time, and change

1. **Is the exercise an in-process library, one service instance, or multiple nodes?**
   Implement the smallest agreed boundary, then explain the replacement for a local
   lock when moving to multiple instances.
2. **Does time affect correctness?**
   Inject a clock and define exact expiry/refill boundary semantics.
3. **Which policies are expected to vary?**
   Allocation, pricing, routing, retries, validation, and provider selection are
   common Strategy candidates.
4. **Must definitions or workflows be versioned?**
   Orders, promotions, templates, and catalog schemas often need historical snapshots.

## Expected solution structure

A strong answer can be presented in this order:

1. **Problem contract** — actors, must-have use cases, non-functional requirements,
   assumptions, and exclusions.
2. **Invariants** — rules that no successful command may violate.
3. **Lifecycle** — named states and guarded transitions; keep independent dimensions
   in separate state fields instead of one cross-product enum.
4. **Domain model** — aggregate roots, entities, value objects, policies, domain
   services, repositories, and external ports.
5. **Command/query API** — inputs, outputs, errors, idempotency, expected version, and
   pagination or filtering where relevant.
6. **Collaboration walkthrough** — one sequence from API to aggregate to repository
   and external ports, followed by the most important failure sequence.
7. **Concurrency boundary** — what is locked/version-checked, why it is sufficient,
   and how unrelated work avoids serialization.
8. **Durability and events** — transaction contents, outbox/event publication,
   reconciliation, audit history, and projection consistency.
9. **Tests** — invalid transitions, exact time boundaries, duplicate commands,
   competing commands, dependency failures, and recovery.
10. **Evolution** — the first design change likely at 10× load or a new business
    policy, and which current abstraction absorbs it.

## Applying SOLID without ceremony

| Principle | Interview-quality application | Common misuse |
| --- | --- | --- |
| SRP | One owner per business invariant or reason to change; orchestration is separate from domain state | One tiny class per method, or an all-knowing `Manager` |
| OCP | Put a real variation point behind a policy interface or data-driven definition | Interfaces for every class “just in case” |
| LSP | Implementations honor the same preconditions, postconditions, errors, and side-effect contract | Subtypes that throw “unsupported” for required operations |
| ISP | Ports expose the smallest capability a caller needs, such as `authorize` or `allocate` | One provider interface containing every possible operation |
| DIP | Domain/application code depends on ports for time, IDs, storage, and remote providers | Dependency injection used only to move a concrete singleton into a constructor |

SOLID is not a checklist to recite. Tie each principle to a concrete change or test.
For example: “Promotion conditions use a `Condition` interface because new campaign
rules are frequent; `OrderLine` remains concrete because its behavior is stable.”

## Patterns: name the pressure before the pattern

| Design pressure | Useful pattern | What must still be explained |
| --- | --- | --- |
| Several interchangeable business algorithms | Strategy | Selection, configuration, and state ownership |
| Behavior changes materially by lifecycle state | State | Legal transitions and whether state objects add value over guarded methods |
| External provider has an incompatible contract | Adapter | Error translation, idempotency, and timeout semantics |
| Ordered validation or processing stages | Chain of Responsibility / pipeline | Stop/continue rules, ordering, and shared context |
| Creation requires validated coherent variants | Factory / Builder | Which invalid intermediate states are prevented |
| A command needs audit, retry, or queueing | Command | Identity, immutable input, and result semantics |
| Post-commit consumers react to a change | Observer plus transactional outbox | Delivery is at least once; consumers must deduplicate |
| Persistence must not leak into the aggregate | Repository / Unit of Work | Transaction boundary and optimistic version check |
| Cross-service workflow has compensating actions | Saga | Unknown outcomes, compensation limits, and reconciliation |

Do not force patterns where a function, enum, or small immutable class is clearer.
“No pattern yet” is a strong answer when the variation pressure is absent.

## Reusable Senior/Staff follow-up questions

### “Where is the transaction boundary?”

**Expected answer:** Name the records changed atomically and the invariant they
protect. In a relational implementation, include the aggregate/version update,
idempotency record, and outbox record in one transaction. A remote provider call is
not part of that database transaction.

### “How do retries avoid repeating side effects?”

**Expected answer:** Scope an idempotency key by operation and caller, persist the
input fingerprint and original result, reject key reuse with different input, and
propagate stable downstream keys. Do not hold only an in-memory “seen” set in a
multi-instance service.

### “What if the remote call times out?”

**Expected answer:** The result is unknown, not failed. Persist an in-progress or
unknown attempt, query/reconcile using the provider key, accept deduplicated
callbacks, and prevent a second incompatible operation until resolution.

### “How do two callers race for the last unit?”

**Expected answer:** Put the check and mutation in one atomic boundary: a local
per-key lock for an in-process exercise, or a conditional update/row lock/atomic
store script in a multi-node design. Demonstrate the losing result explicitly.

### “Why this interface?”

**Expected answer:** Identify the independent change: provider, policy, persistence,
time, or ID generation. If there is no independent variation or test seam, keep the
type concrete.

### “How are events published reliably?”

**Expected answer:** Write an outbox record with the aggregate change, publish it
asynchronously, use stable event IDs, and make consumers idempotent. Never claim an
ordinary in-memory Observer guarantees durable delivery.

### “How would you test this?”

**Expected answer:** Unit-test aggregate transitions and policies with injected time;
contract-test adapters; integration-test transaction/idempotency constraints; and run
deterministic race tests with barriers rather than sleeps.

## Senior versus Staff depth

| Area | Strong Senior | Strong Staff |
| --- | --- | --- |
| Scope | Completes the agreed vertical slice | Identifies organizational/service ownership and prevents scope leakage |
| Correctness | Protects invariants and handles retries | Defines reconciliation, migration, and operational recovery paths |
| Extensibility | Adds interfaces at demonstrated variation points | Distinguishes near-term extension from speculative platform building |
| Concurrency | Chooses a correct atomic boundary | Explains hot-key behavior, multi-node replacement, and rollout safety |
| Data | Models versions and audit where required | Separates source of truth, projections, retention, and compatibility |
| Communication | Defends a coherent design | Navigates alternatives and makes the decision criteria explicit |

## Scoring rubric

Score each dimension from 0 to 4.

| Dimension | 0–1 | 2 | 3 | 4 |
| --- | --- | --- | --- | --- |
| Scope and requirements | Assumes or wanders | Captures happy path | Clear scope and exclusions | Prioritizes ambiguity by design impact |
| Domain model | Data bags or god object | Plausible classes | Cohesive behavior and value objects | Ownership maps directly to invariants |
| API and lifecycle | CRUD only | Some commands/states | Explicit contracts and guarded transitions | Version/idempotency/error semantics are complete |
| SOLID and patterns | Name-dropping | Some useful separation | Principles tied to real change | Simpler alternatives and rejected patterns defended |
| Concurrency and failure | “Add a lock” | Handles one race | Atomic boundary, retry, timeout | Unknown outcomes, recovery, and multi-node evolution |
| Testing and communication | No proof | Basic examples | Deterministic boundary tests | Prioritized narrative plus operability/migration proof |

A score of 3 in most dimensions is a strong Senior answer. Staff evidence requires
several 4s, especially in ownership, failure recovery, evolution, and communication;
adding more services or patterns does not raise the score by itself.

## Red flags to avoid

- Starting with `User`, `Manager`, and `Database` classes before clarifying use cases.
- A god service that validates, mutates, persists, calls providers, and sends events.
- Treating every noun as an entity and every concrete class as an interface.
- Using `double` for money or wall-clock time for elapsed-duration algorithms.
- Saying “exactly once” without explaining transaction and deduplication boundaries.
- Treating a timeout as a definite failure and blindly retrying a charge or shipment.
- Choosing Singleton for convenience and hiding mutable global state.
- Drawing microservices when the question asks for object collaboration.
- Ignoring invalid transitions, competing commands, and historical snapshots.
- Ending without tests, rejected alternatives, or a summary of trade-offs.

## Python in a one-hour LLD round

Use Python to make the domain decisions visible, not to compress the design into
untyped dictionaries and one large function.

- Use frozen `dataclass` value objects and enums for identity, money, commands,
  results, and lifecycle. Add `slots=True` only when the agreed runtime is Python
  3.10+; these runnable packs intentionally remain compatible with Python 3.9.
  Use `Decimal` or integer minor units for money.
- Use `Protocol` or a small abstract base only for a demonstrated policy/provider/
  repository seam. Concrete stable domain objects do not need ceremonial interfaces.
- Inject time and ID callables. Tests should advance a manual clock rather than sleep.
- Protect one invariant with one explicit `Lock`/`RLock` or repository compare-and-set.
  The GIL does not make a multi-step check-then-mutate sequence a business transaction.
- Code the invariant-heavy aggregate/policy method and one deterministic race or retry
  test. Explain HTTP, ORM, queues, outbox tables, distributed stores, metrics, and
  deployment rather than typing framework boilerplate.
- Return typed domain outcomes for rejection, conflict, and unknown external results;
  do not reduce every failure to `False` or a broad exception.

The eight featured packs apply this boundary consistently: their Python modules are
runnable core answers, while the Staff discussion covers production adapters and
evolution.

## Practice loop

1. Pick one problem and set a 60-minute timer.
2. Speak the clarifying questions aloud and commit to assumptions by minute 10.
3. Produce one class/ownership diagram, one state machine, and one success/failure
   sequence—not every UML diagram.
4. Implement or pseudocode the invariant-heavy method, not boilerplate controllers.
5. Use the rubric to review the answer and rewrite the weakest section.
6. Compare against the problem's reference guide and runnable demo, then explain one
   design choice you would change for a multi-instance production deployment.

## How the eight problem packs integrate variations

Each featured problem is one canonical learning page, not a separate answer plus a
reference appendix. Within that page, `Interview variations and expected solutions`
turns every materially different source prompt into a numbered question. Each
variation contains its scope delta, Senior solution, Staff extension, and the part of
the runnable canonical design that applies or must change.

Language ports, byte-identical copies, and aliases collapse into one variation.
Partial source fragments remain useful code-review inputs, but the page corrects
missing concurrency, ownership, or failure behavior instead of presenting a fragment
as a complete solution. Exact provenance stays inline with the variation it informed;
unlicensed source is summarized rather than copied.

Continue with the [problem bank](../Problems/) or start with the eight fully runnable
Senior/Staff reference problems highlighted in its opening commerce and infrastructure
sections.
