# Snake Video Game

## Interview Prompt

Design the real-time Snake video game: a snake advances on ticks, direction changes cannot reverse into its neck, food grows the body, collisions end the session, and food placement never overlaps the snake. This is distinct from [Snake and Ladder](../snake_ladder/README.md).

## Core Model

`Cell`, `Direction`, `Snake` (ordered body plus occupied set), `Board`, `Food`, `FoodPlacementPolicy`, `GameClock`, and `SnakeGame` separate deterministic state transitions from scheduling and rendering.

## Invariants

- Body cells are ordered head-to-tail and match the occupied set.
- One tick creates one next head; a 180-degree turn is rejected when length exceeds one.
- Moving into the current tail is legal only when the tail moves away on that tick.
- Eating grows exactly once and places new food only on a free cell.
- A wall/self collision causes one terminal transition; later ticks/input have no effect.

## API and Tick Solution

```java
void requestDirection(Direction direction, long inputSequence);
TickResult tick(long expectedTick);
GameSnapshot snapshot();
void pause();
void resume();
```

The game loop owns mutation. On each tick, consume at most one valid pending direction, calculate the next head, determine whether food is eaten, remove the tail first only when not growing, test collision with the resulting occupied set, then add the head and possibly place food. Inject clock/random policy for deterministic tests. UI input threads enqueue commands rather than modifying the snake; stale tick IDs are idempotent.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/SnakeGame/` — Java board/cell/snake/game model. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/SnakeGame)
2. `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/SnakeGame2/` — interface, food-service, and factory variation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/SnakeGame2)
3. `References/kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/snakegame/` — alternate Java/Maven collection model. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/snakegame)

## Follow-Ups and Status

Wraparound walls, obstacles, multiple foods, levels/speed, deterministic replay, multiplayer, and AI. The canonical design preserves all three actual variations in the unchanged no-root-license clone.
