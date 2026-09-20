# Cricket Information System

## Problem

Design a CricInfo-style domain for fixtures, live scoring, scorecards, commentary, and cricket statistics.

## Required behavior

- Manage teams, squads, players, venues, officials, series, and scheduled matches.
- Represent formats and match rules without scattering format-specific conditionals.
- Record each delivery and derive overs, innings totals, wickets, partnerships, and player figures.
- Publish live score and match-status changes to observers.
- Expose fixtures, results, scorecards, commentary, and searches for matches, teams, and players.
- Correct an incorrectly entered delivery while keeping a visible audit trail.

## Core model

`Series`, `Match`, `Innings`, `Over`, `Delivery`, `DeliveryOutcome`, `Team`, `Player`, `Scorecard`, `CommentaryEntry`, and `MatchStatus`.

Treat delivery events as the authoritative history and derive score summaries from them. A scorer command and a spectator query are different responsibilities.

## Invariants and edge cases

- Legal-ball counting must distinguish wides, no-balls, and other extras.
- A delivery can contain runs and a dismissal; dismissal attribution must follow the rule type.
- Only valid state transitions may start, pause, complete, or abandon a match.
- Concurrent readers must never observe a partially applied delivery.

## Design exercise

Demonstrate a normal over, extras, a wicket, an event correction, innings completion, live subscriptions, and a final scorecard.
