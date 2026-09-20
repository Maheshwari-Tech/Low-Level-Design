# Voting System

## Problem

Design a configurable election domain that registers eligible voters, accepts one secret ballot per voter, and produces an auditable result.

## Required behavior

- Create an election with contests, candidates or options, eligibility rules, and an open/close window.
- Register voters and issue a single-use authorization that does not reveal ballot choices.
- Validate and cast a ballot atomically.
- Support a declared voting rule, such as single-choice or ranked choice, behind a strategy.
- Prevent early result disclosure while allowing authorized turnout reporting.
- Close an election, tally immutable ballots, and publish a signed result summary.
- Record administrative actions and verification evidence without storing a voter-to-choice link.

## Core model

`Election`, `Contest`, `Candidate`, `Voter`, `EligibilityPolicy`, `VotingCredential`, `Ballot`, `Selection`, `TallyStrategy`, and `Result`.

Authentication, ballot secrecy, and tallying are distinct concerns. In a real election, cryptography and independent certification are mandatory; this exercise focuses on object boundaries and invariants.

## Invariants and edge cases

- A credential can cast at most one accepted ballot for its election.
- Accepted ballots are append-only and cannot be silently replaced.
- Only eligible selections under the election's rule may be tallied.
- Handle simultaneous submissions, expired credentials, election closure races, candidate withdrawal, tie policy, and recount.

## Design exercise

Demonstrate setup, eligibility rejection, a successful anonymous vote, duplicate prevention, closure, tally, tie handling, and audit verification.
