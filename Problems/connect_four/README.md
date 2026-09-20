# Connect Four

## Interview Prompt

Design Connect Four for configurable board dimensions and connect length. Players alternate dropping discs into non-full columns; gravity chooses the row and the first horizontal, vertical, or diagonal line wins.

## Model and Invariants

`Board`, `Cell`, `Column`, `Disc`, `Player`, `Move`, `WinPolicy`, and `ConnectFourGame` form the core.

- A move belongs to the current player and a valid non-full column.
- A disc occupies the lowest empty cell; occupied cells never move.
- A win is detected through the last move in four direction pairs.
- No move is accepted after `WON` or `DRAW`.
- A command ID or expected version prevents duplicate/concurrent drops.

## API Sketch and Solution

```java
MoveResult drop(PlayerId player, int column, String commandId, long expectedVersion);
Set<Integer> playableColumns();
GameSnapshot snapshot();
Optional<GameResult> result();
```

Store each column's next free row for `O(1)` placement. After a drop, count same-colour discs from the new cell in both directions for horizontal, vertical, and the two diagonals; a total including the new disc at least equal to `connectLength` wins. If no column remains, declare a draw. Keep all mutation in the game aggregate and serialize it per game.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Problem_set_2/connect4/` — first Java board/ball/DAO implementation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/connect4)
2. `References/kumaransg-LLD/Low_level_Problem_set_2/connect4 2/` — materially different second source tree retained as another approach. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/connect4%202)

`Low_level_Problem_set_2/connect4.zip` duplicates the extracted first tree and is not counted as a third solution.

## Follow-Ups and Status

AI search, undo/replay, spectators, timers, arbitrary `M × N × K`, and distributed turn fencing. The canonical page supplies the interview solution; both actual Java variations remain in the exact no-root-license clone.
