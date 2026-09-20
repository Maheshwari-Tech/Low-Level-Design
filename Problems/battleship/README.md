# Battleship

## Interview Prompt

Design a two-player Battleship game with validated fleet placement, hidden opponent state, alternating attacks, hit/sunk detection, and a terminal winner.

## Core Model

`Coordinate`, `ShipType`, `Ship`, `Placement`, `OceanBoard`, `TrackingBoard`, `Fleet`, `Attack`, `Player`, and `BattleshipGame` separate authoritative hidden state from each player's public knowledge.

## Invariants

- Every ship occupies its declared number of contiguous horizontal or vertical cells.
- Ships stay in bounds and do not overlap; optional adjacency rules are explicit.
- A coordinate is attacked at most once by a player, or a retry returns the stored result.
- Opponents see only miss/hit/sunk information allowed by the rules.
- A ship is sunk when every occupied cell is hit; the game ends when one fleet is fully sunk.

## API and Solution

```java
void placeShip(PlayerId player, ShipId ship, Coordinate bow, Orientation orientation);
void ready(PlayerId player, long expectedVersion);
AttackResult attack(PlayerId player, Coordinate target, String commandId);
PublicGameView viewFor(PlayerId viewer);
```

Validate all placements before entering `IN_PROGRESS`. An attack checks the current player and command ID, consults the opponent's private occupancy map, records the shot, updates ship damage, derives public result, and advances the turn atomically. Return player-specific DTOs so serialization cannot leak unhit ship coordinates. Serialize attacks per game and persist command results for network retries.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Problem_set_2/battleship/` — Java board, ship, player, constants, and DAO implementation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/battleship)

## Follow-Ups and Status

Salvo rules, random placement, AI, fog-of-war events, reconnect, timers, cheating prevention, and spectators. The canonical page completes the interview design; the actual Java variation remains in the local no-root-license clone.
