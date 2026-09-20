# Othello / Reversi

## Interview Prompt

Design Othello on an `N × N` board: a player places a disc only when it brackets at least one opposing line, flips every bracketed disc, passes when no legal move exists, and wins by final disc count.

## Model and Rules

Use `Position`, `Disc`, `Board`, `Move`, `Player`, `Direction`, `MoveValidator`, and `OthelloGame`. The board is the aggregate-owned state; move validation returns the exact set of positions to flip.

- A legal move targets an empty cell and captures one or more discs in at least one of eight directions.
- A direction captures only when one or more adjacent opponent discs end at the current player's disc.
- Place and all flips commit atomically.
- A player with no legal move passes; two consecutive passes or a full board ends the game.
- Disc counts equal occupied cells and terminal scoring is deterministic.

## API Sketch

```java
Set<Position> legalMoves(PlayerId player);
MoveResult move(PlayerId player, Position position, long expectedVersion);
MoveResult pass(PlayerId player, long expectedVersion);
GameSnapshot snapshot();
GameResult result();
```

## Design Solution

For every direction, walk from the candidate cell while seeing opponent discs, then accept that collected line only if it terminates at the mover's disc. Concatenate all accepted lines, reject when empty, and apply placement/flips in one aggregate transition. Cache legal moves only by board version. Serialize commands per game or compare versions so two network moves cannot both claim the same turn.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/OthelloGame/Design1.java` — first single-file Java design. [Pinned source](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/OthelloGame/Design1.java)
2. `References/kumaransg-LLD/Low_level_Design_Problems/OthelloGame/Design2.java` — materially separate second design; retain both for comparison. [Pinned source](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/OthelloGame/Design2.java)

## Follow-Ups and Status

Move hints, undo/replay, timers, AI strategy, spectators, persistence, and arbitrary even board sizes. This page supplies the canonical interview reasoning; both actual designs remain in the unmodified no-root-license clone.
