# Problem: Battleship

Build an in-memory Battleship game for one player attacking a hidden fleet.

The goal of the exercise is to demonstrate domain modelling, clean boundaries,
testability, and explicit trade-offs within a 90-minute implementation window.

## Functional requirements

1. Place the configured ships on an 8×8 board.
2. Support horizontal and vertical placement.
3. Reject ships outside the board or overlapping another ship.
4. Fire at a cell and return miss, hit, sunk, and won outcomes.
5. Reject duplicate shots and placement after firing starts.
6. Reset the game.

## Game configuration

- Board: 8 rows × 8 columns.
- Coordinates: zero based; `(0, 0)` is the top-left cell.
- Fleet: Destroyer (2), Cruiser (3), Battleship (4).
- A ship occupies consecutive cells starting at the supplied origin.

## Scope

- One game and one board are sufficient.
- Ship adjacency is allowed.
- The API reveals ship positions because this is a single-player demonstration.
- Computer strategy, multiple players, turns, and persistence are out of scope.

## Evaluation examples

- A length-4 ship starting at column 6 horizontally is rejected.
- A shot against an occupied cell is a hit.
- Hitting every cell of one ship reports it as sunk.
- Hitting every fleet cell wins the game.

## Non-functional expectations

- Rules must be enforced by domain objects, not by API handlers.
- Invalid commands must fail without partially changing game state.
- Storage must be replaceable without changing the game rules.
- Core rules must be covered by automated tests.
