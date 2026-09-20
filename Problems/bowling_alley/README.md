# Bowling Alley

## Interview Prompt

Design a bowling-alley session that assigns players to a lane, records rolls, computes ten-pin scores correctly, and reports the winner. Keep scoring policy separate enough to support a different ruleset.

## Scope and Requirements

1. Create a game for a lane and an ordered set of players.
2. Record legal rolls and advance frames/players automatically.
3. Score open frames, spares, strikes, and tenth-frame bonus rolls.
4. Reject rolls that exceed remaining pins or occur after game completion.
5. Expose a live scorecard with pending bonuses represented honestly.
6. Finish once every player has completed the final frame and determine rankings.

## Core Model

| Type | Responsibility |
| --- | --- |
| `BowlingGame` | Aggregate root for players, turn cursor, lane, and lifecycle |
| `PlayerGame` | One player's frames and current roll position |
| `Frame` | Rolls plus open/spare/strike/completion rules |
| `Roll` | Validated knocked-pin value and sequence |
| `ScorePolicy` | Computes resolved and provisional frame scores |
| `Lane` | Physical-lane identity and active-game assignment |
| `Scorecard` | Read model for frame marks, totals, and winner |

## Invariants

- A normal frame contains at most two rolls and at most ten knocked pins.
- A strike ends frames one through nine immediately.
- The tenth frame grants only the bonus rolls earned by a spare or strike.
- A bonus is counted once even when consecutive strikes overlap its look-ahead window.
- One active game owns a lane at a time; a completed game accepts no roll.

## API Sketch

```java
GameId startGame(LaneId lane, List<PlayerId> players);
RollResult roll(GameId gameId, PlayerId player, int pins, String idempotencyKey);
Scorecard scorecard(GameId gameId);
BowlingGame complete(GameId gameId, long expectedVersion);
```

## Scoring Solution

Store the raw roll sequence as truth. For each frame, an open frame scores its two rolls, a spare scores ten plus the next roll, and a strike scores ten plus the next two rolls. A frame total remains pending until required future rolls exist. The aggregate owns turn advancement and pin reset; a pure `ScorePolicy` derives totals, which makes boundary cases easy to test and enables alternate rules without changing session orchestration.

Serialize roll commands per game or use an expected aggregate version. Persist the roll and new cursor atomically. An idempotency key prevents a retried lane-controller event from knocking pins down twice.

## Source-Backed Solution Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart/` — Java/Maven player, round, game, and scorekeeper approach. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart)
2. `References/kumaransg-LLD/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart-Interview/` — C#/.NET Core factory-and-strategy variation. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart-Interview)
3. `References/kumaransg-LLD/Low_level_Problem_set_2/bowlingAlley/` — alternate Java DAO/model implementation. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Problem_set_2/bowlingAlley)

## Essential Test Cases

- Gutter game: `0`.
- All ones: `20`.
- Spare followed by `3`: first frame `13`.
- Perfect game: `300`.
- Strike in the tenth with two bonus rolls.
- Duplicate lane event returns its original result.

## Implementation Status

The canonical page gives the interview-ready scoring and aggregate solution. All three actual language variations remain available in the exact local clone.
