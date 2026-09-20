# Tic-Tac-Toe

## Problem

Design a two-player Tic-Tac-Toe game on a 3 x 3 board. Players alternate `X` and `O`; the game rejects invalid moves and ends on a row, column, or diagonal win, or a full-board draw.

## Requirements

- Initialize and display an empty board.
- Create two players with distinct symbols.
- Enforce player turns.
- Reject out-of-bounds and occupied cells without advancing the turn.
- Detect all win conditions after a successful move.
- Detect a draw only when the board is full and there is no winner.
- Stop accepting moves after `WON` or `DRAW`.

## Core Model

- **`TicTacToeGame`**: game loop, turn order, rules, and status.
- **`Board`**: owns the cell grid, move validation, placement, display, and win checks.
- **`Player`**: immutable name and symbol.
- **`Move`**: optional immutable row, column, and player value for history or undo.
- **`GameStatus`**: `IN_PROGRESS`, `WON`, or `DRAW`.

## Data Structures and Invariants

- `char[N][N]` for cells; the default game uses `N = 3`.
- An optional `List<Move>` for history.
- An enum for status; player symbols can also be an enum if more type safety is desired.

Each accepted move fills exactly one empty cell using the current player's symbol. `WON` and `DRAW` are terminal and mutually exclusive. For a generalized N x N board, row/column counters avoid rescanning the board, but the simple grid scan is clearer for 3 x 3.

## Design Notes

The model primarily uses encapsulation and composition. A Factory may validate player creation, and a Template Method can support a family of turn-based grid games, but neither pattern is required for the basic game.

## Game Flow

1. Display the board and prompt the current player.
2. Parse and validate row and column.
3. Place the symbol; retry the same player if the move is invalid.
4. Check winner first, then full-board draw.
5. Otherwise switch players and repeat.

## Implementation Status

The baseline is implemented in [`com/example/lld/tic_tac_toe`](com/example/lld/tic_tac_toe). It includes a console demo, programmatic `playMove`, defensive board snapshots, terminal-state enforcement, and win detection for configurable square board sizes. Move history, undo, an AI player, and a GUI remain extensions.

```bash
javac -d /tmp/lld-build Problems/tic_tac_toe/com/example/lld/tic_tac_toe/*.java
java -cp /tmp/lld-build com.example.lld.tic_tac_toe.Main
```
