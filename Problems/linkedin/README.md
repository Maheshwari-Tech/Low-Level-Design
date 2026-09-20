# Professional Network (LinkedIn)

## Problem

Design a professional social network with profiles, connections, publishing, messaging, and job discovery.

## Required behavior

- Create a profile containing experience, education, skills, and privacy settings.
- Send, accept, decline, withdraw, and remove connection relationships.
- Follow members or organizations and publish posts with comments and reactions.
- Produce a feed from visible activity without exposing private content.
- Search people, companies, posts, and jobs using structured criteria.
- Post jobs, submit applications, and track an applicant's state.
- Create direct conversations and user notifications through separate services.

## Core model

`Member`, `Profile`, `Experience`, `Skill`, `ConnectionRequest`, `Connection`, `Company`, `Post`, `Comment`, `Job`, `Application`, and `Conversation`.

Model connection requests and accepted connections separately. Centralize visibility checks in an authorization policy instead of filtering ad hoc in every query.

## Invariants and edge cases

- A pair of members has at most one active connection/request relationship.
- Users cannot connect to themselves or view content outside its audience.
- Application transitions must be ordered and attributable to the correct actor.
- Handle blocks, deleted content, concurrent request acceptance, pagination, and notification deduplication.

## Design exercise

Demonstrate profile updates, the connection lifecycle, audience-filtered feeds, job application transitions, blocking, and search.

See the general [Social Network](../social_network/) problem for shared primitives.
