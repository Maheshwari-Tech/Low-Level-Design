# Blackjack

## Interview Prompt

Design a blackjack table that creates and shuffles a shoe, accepts wagers, deals hands, applies player actions and dealer rules, and settles each round exactly once.

## Core Model

| Type | Responsibility |
| --- | --- |
| `Card`, `Rank`, `Suit` | Immutable card identity and blackjack value |
| `Shoe` | One or more decks, shuffle/cut, and deal cursor |
| `Hand` | Cards plus hard/soft total calculation |
| `PlayerSeat` | Participant, wager, hand(s), and round state |
| `DealerHand` | Dealer visibility and configured hit/stand policy |
| `BlackjackRound` | Aggregate for dealing, turns, outcomes, and settlement |
| `PayoutPolicy` | Blackjack, win, push, insurance, and surrender amounts |

## Invariants

- A physical card is dealt at most once before the shoe is reset.
- Wagers are reserved before the initial deal and settled/refunded once.
- Only the current seat performs a legal action; busted/stood hands cannot act.
- An ace counts as eleven only while the hand total remains at most twenty-one.
- Dealer play begins after all live player hands finish and follows one configured rule.

## API and Flow

```java
RoundId openRound(TableId table, List<Bet> bets, String idempotencyKey);
RoundSnapshot dealInitial(RoundId roundId, long expectedVersion);
ActionResult act(RoundId roundId, SeatId seat, PlayerAction action, long version);
RoundResult playDealerAndSettle(RoundId roundId, long expectedVersion);
```

Reserve funds and open the round atomically. The aggregate deals in seat order, exposes only the dealer up-card, advances legal turns, then runs the dealer policy and derives immutable outcomes. Send ledger settlement commands through an idempotent port; an unknown payment outcome leaves a recoverable `SETTLING` state rather than paying twice. Serialize commands per table/round.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Design_Problems/designBlackJack/` — Java deck, card, hand, player, dealer, and game implementation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/designBlackJack)

`Low-Level-Design-1/designBlackJack` is an exact duplicate, not a second solution.

## Follow-Ups and Status

Splits, double-down, insurance, multiple decks, cut-card reshuffle, table limits, audit/replay, and tournament play. The canonical page supplies the interview design; upstream code stays in the exact no-root-license clone.
