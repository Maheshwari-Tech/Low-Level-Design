# Chess Game

## Problem

Design a two-player chess game that enforces legal play and keeps the rules extensible and testable.

## Required behavior

- Initialize the standard 8x8 board and the complete set of pieces for each side.
- Alternate turns and reject moves from the wrong player or to an illegal square.
- Delegate piece movement rules while letting the board validate path obstruction and king safety.
- Support capture, castling, en passant, and pawn promotion.
- Detect check, checkmate, stalemate, resignation, and draw conditions.
- Retain move history so a game can be replayed or exported.

## Core model

`Game`, `Board`, `Square`, `Player`, `Piece`, concrete piece types, `Move`, `MoveResult`, `Color`, and `GameStatus`.

A move should be evaluated against a snapshot or be reversible: testing whether a king remains safe must not corrupt the live board. Avoid a single switch containing every piece rule.

## Invariants and edge cases

- Exactly one king of each color remains on the board.
- A player cannot make a move that leaves their own king in check.
- Castling rights, en-passant eligibility, and promotion choice belong to game state, not UI state.
- Repeated-position and no-progress draw rules require historical state, not only the current board.

## Design exercise

Demonstrate normal movement, blocked movement, capture, check rejection, castling, promotion, checkmate, and stalemate. Make the clock and input/output adapters optional extensions to the domain.
