# Deck of Cards

## Interview Prompt

Design reusable card/deck primitives that support different deck compositions, unbiased shuffling, dealing, discard piles, and game-specific ranking without embedding poker or blackjack rules in `Card`.

## Core Model and Invariants

`Card<R,S>` is immutable. `DeckDefinition` produces a validated card set; `Deck` owns ordered remaining cards; `DiscardPile`, `ShuffleStrategy`, `RandomSource`, and `Hand` complete the reusable model.

- A card identity appears at most once in a standard deck definition.
- `remaining + dealt + discarded` equals the created deck size unless the game explicitly removes cards.
- Dealing more cards than remain fails atomically.
- Shuffle uses injected randomness and a documented unbiased algorithm such as Fisher-Yates.
- Rank comparison belongs to a game policy, not to a universal `Card.compareTo`.

## API Sketch

```java
Deck create(DeckDefinition definition);
void shuffle(ShuffleStrategy strategy, RandomSource random);
List<Card> deal(int count);
Map<PlayerId, List<Card>> dealRoundRobin(List<PlayerId> players, int cardsEach);
void discard(Collection<Card> cards);
DeckSnapshot snapshot();
```

## Design Solution

Keep card identity/value objects separate from the mutable deck aggregate. Fisher-Yates swaps each suffix position with a uniformly selected remaining index. A round-robin deal validates the total request first, then removes in player order so failure cannot partially deal. For multi-threaded callers, serialize the deck or use one owning game actor; a global lock across unrelated games is unnecessary.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/deck-of-cards/` — compact Java card, deck, suit, and rank design with README. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/low-level-design-3/deck-of-cards)

## Follow-Ups and Status

Multiple decks/shoes, jokers, deterministic replay seeds, hidden information, serialization, and cryptographically auditable shuffles. This maintained design complements the domain-specific [Blackjack](../blackjack/README.md); upstream code remains unchanged in the local clone.
