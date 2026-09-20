# Battleship Design Decisions

This document explains why the solution is shaped this way. It separates
intentional decisions from shortcuts made for a 90-minute exercise.

## Design goals

The implementation optimizes for four things:

1. Correct game invariants.
2. Code that can be understood class by class.
3. Tests that run without external infrastructure.
4. Clear replacement points for storage and transport.

It does not optimize for distributed scale, multiple games, or exhaustive
framework abstraction.

## Object responsibilities

| Class | Owns | Does not own |
|---|---|---|
| `BattleshipGame` | Game lifecycle, legal state transitions, fleet completeness, win outcome | HTTP, persistence, board geometry |
| `Board` | Boundaries, occupied cells, overlap checks, shots, hit detection | Game lifecycle, fleet configuration |
| `Ship` | Size, occupied cells, received hits, sunk state | Placement geometry, HTTP responses |
| `Cell` | A coordinate value | Validation against a particular board |
| `ShipFactory` | Supported fleet and ship construction | Placement and game state |
| `GameService` | Application use cases and repository coordination | Domain rule implementation |
| `GameController` | Converting use-case results to the public JSON shape | Game rules |
| `GameRepository` | Persistence contract | Domain behavior |

This split keeps behavior with the state it protects. For example, `Ship`
calculates whether it is sunk; a controller never counts hits.

## Decision 1: `BattleshipGame` is the aggregate root

All state-changing commands enter through `BattleshipGame`. It coordinates the
board and ships while preserving lifecycle rules.

An alternative was to let the service call `Board.place_ship()` and
`Board.receive_shot()` directly. That would let callers bypass rules such as
"the complete fleet must be placed before firing." The aggregate root provides
one consistency boundary.

## Decision 2: use composition, not a ship subclass hierarchy

Destroyer, Cruiser, and Battleship currently differ only by name and size.
Creating three subclasses would add types without adding behavior. A
configuration-backed `ShipFactory` keeps construction in one place.

Subclassing becomes useful only if ship types acquire different rules or
behavior.

## Decision 3: separate lifecycle rules from board geometry

`BattleshipGame` answers whether a command is legal at the current stage.
`Board` answers whether coordinates fit and whether cells overlap. Combining
them would produce one large class with two reasons to change.

## Decision 4: immutable value objects, mutable entities

`Cell`, `Shot`, and `ShotOutcome` are frozen dataclasses because they describe
values and results. `Ship`, `Board`, and `BattleshipGame` are mutable because
they have identity and a lifecycle.

Collections are exposed as tuples, frozen sets, or copies so callers cannot
mutate internal state accidentally.

## Decision 5: explicit domain exceptions

Expected rule failures have named exception types. This lets tests assert the
exact failure and lets the HTTP adapter map input errors to `400` and state
conflicts to `409`. Domain code remains independent of HTTP.

A single generic exception with message parsing was rejected because messages
are not a stable machine-readable contract.

## Decision 6: repository boundary with an in-memory adapter

The exercise requires no database, so `InMemoryGameRepository` keeps setup
small. `GameService` depends on the `GameRepository` abstraction so a durable
implementation can replace it without changing game rules.

The trade-off is one extra interface for a single stored object. It is retained
because persistence replacement is a realistic requirement and demonstrates
dependency inversion without introducing a framework.

## Decision 7: manual dependency injection

`main.py` constructs the object graph explicitly. A dependency-injection
framework would add setup and hide construction for little benefit at this
size. Constructor injection is enough to make dependencies visible and tests
isolated.

## Decision 8: lock aggregate mutations in memory

`BattleshipGame` uses a re-entrant lock so one process cannot interleave two
mutations inside a placement or shot. The repository protects replacement of
the current game during reset.

This is not a distributed concurrency solution. Multiple workers would have
independent memory and locks. The production design uses durable state plus a
version check, described in `PRODUCTION_READINESS.md`.

## SOLID, applied pragmatically

- **Single responsibility:** domain lifecycle, geometry, construction,
  persistence, application orchestration, and HTTP mapping are separate.
- **Open/closed:** fleet configuration and repository implementations can vary
  without modifying shooting and placement rules.
- **Liskov substitution:** any `GameRepository` implementation must preserve
  `get` and `save` semantics.
- **Interface segregation:** the repository exposes only the two operations the
  service needs.
- **Dependency inversion:** `GameService` depends on `GameRepository`, while
  `main.py` selects the in-memory implementation.

SOLID is used to clarify change boundaries, not to maximize the number of
interfaces or classes.

## Patterns intentionally used

- Aggregate Root: `BattleshipGame`
- Repository: `GameRepository`
- Factory: `ShipFactory`
- Service Layer: `GameService`
- Controller: `GameController`
- Composition Root: `create_app`

No Strategy, Observer, Command, or event bus is added because the current
requirements do not need interchangeable algorithms or asynchronous workflows.

## Known exercise-level compromises

- There is one game, so endpoints do not contain a `game_id`.
- The board is returned with ship positions for the demonstration UI.
- State is lost on restart.
- A reset racing with an in-flight request is not guaranteed to be linearizable.
- API responses are simple dictionaries instead of versioned public DTOs.
- Authentication, rate limits, telemetry, and deployment assets are out of the
  coding-round implementation.

These are conscious scope boundaries rather than hidden assumptions.
