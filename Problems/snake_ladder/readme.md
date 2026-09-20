# Snake and Ladder Game

## Problem

Implement a turn-based Snake and Ladder game on a 100-square board. Players roll a six-sided die, immediately follow a snake or ladder after landing, and the first player to reach exactly 100 wins.

## Requirements

- Support multiple players; the usual game validates 2-4 participants.
- Keep a player's position between 0 and 100.
- Roll a value from 1 through 6.
- Apply predefined snake-head-to-tail and ladder-bottom-to-top transitions.
- Preserve the current position when a roll overshoots 100.
- Rotate turns in insertion order.
- Stop when a player reaches exactly 100.

## Current Design

- **`SnakeAndLadderGame`** owns the board configuration, players, dice RNG, console loop, and win rule.
- The package-private **`Player`** class stores name and position.
- `Map<Integer, Integer>` stores snakes and ladders for average O(1) landing lookup.
- `List<Player>` stores players; modular indexing rotates turns.
- `Random` supplies dice values.

Snakes and ladders are applied immediately after the move and before checking the win condition. Their positions are hardcoded so the demo is repeatable apart from dice rolls.

## Trade-offs

- The compact two-class implementation is easy to follow but couples input, randomization, board rules, and game flow.
- Extracting **`Board`**, **`Dice`**, and **`GameStatus`** would improve testing and allow configurable board sizes or dice.
- Injecting a seeded/test dice avoids nondeterministic tests.
- Loading transitions from configuration allows validation that snake heads move backward, ladders move forward, and no start has two transitions.

## Implementation Status

Implemented:

- a 100-square board with predefined snakes and ladders;
- dynamic `addPlayer`, sequential turns, dice rolls, exact-win behavior, and an interactive two-player demo.

Gaps and extensions:

- the code does not enforce a 2-4 player bound or reject an empty player list;
- board configuration and dice are not injectable;
- there is no separate Board/Dice model, game-status enum, replay, GUI, file configuration, or network multiplayer.

## Run

Requires Java 17 and Maven:

```bash
cd Problems/snake_ladder
mvn compile
java -cp target/classes com.example.lld.snake_ladder.SnakeAndLadderGame
```
