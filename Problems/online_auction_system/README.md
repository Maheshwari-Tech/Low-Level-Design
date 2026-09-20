# Online Auction System

## Problem

Design a marketplace where sellers list items and bidders compete under explicit auction rules.

## Required behavior

- Create draft listings with item details, start/end time, reserve price, and bid increment.
- Publish, start, close, and cancel an auction through valid state transitions.
- Accept bids only from eligible users while the auction is open.
- Atomically determine whether a bid becomes the current winning bid.
- Support automatic proxy bidding up to a bidder's private maximum.
- Notify interested users when they are outbid or an auction closes.
- Select the winner, handle an unmet reserve, and create a payment obligation.

## Core model

`Auction`, `Listing`, `Bid`, `Bidder`, `Seller`, `Money`, `AuctionPolicy`, `AuctionStatus`, and `PaymentObligation`.

Use an injected clock and a serialized decision per auction. Store accepted bid events and derive the displayed price and leader from the policy.

## Invariants and edge cases

- Bids are immutable, ordered, and cannot be accepted after closure.
- The displayed price respects current price, minimum increment, reserve, and proxy maxima.
- Retrying the same bid command must not create a second bid.
- Handle simultaneous bids, tied maxima, seller cancellation, clock boundary, and payment default.

## Design exercise

Demonstrate normal bidding, proxy competition, a last-instant race, reserve not met, closure notification, cancellation, and a duplicate request.

The repository's [Mediator pattern auction example](../../Patterns/behavioral/mediator/) is a small interaction example, not a substitute for this domain.
