# Social Network

## Problem

Design a social network in which users create profiles, form connections, publish posts, react to content, and receive a personalized feed. Keep the implemented direct-friend demo distinct from broader Facebook-like requirements.

## Implemented Scope

- Register an in-memory user with ID and name.
- Add a bidirectional friendship and remove it from an individual `User`.
- Create a text post and associate it with its author.
- Increment a post's like count.
- Return the user's and direct friends' posts sorted newest first.

## Core Classes

- **`SocialNetwork`**: facade with user and post registries and feed aggregation.
- **`User`**: profile, friend IDs, and authored posts.
- **`Post`**: ID, author ID, text, timestamp, and aggregate likes.

`Map<String, User>` and `Map<String, Post>` provide average O(1) lookup. `Set<String>` prevents duplicate friend IDs, and `Set<Post>` prevents duplicate post references in a user's feed. The feed performs fan-out on read and sorts the combined set by timestamp.

## Invariants and Trade-offs

- A friendship created through `SocialNetwork.addFriend` is bidirectional.
- A post ID should identify one post; the current map would overwrite a duplicate ID without cleaning the prior author's set.
- `likePost` stores only a count, so it cannot prevent one user from liking repeatedly.
- Fan-out on read is simple but grows with friend count and post history; ranked or cached feeds need pagination and a stable tie-breaker.
- The in-memory design is fast for a demo but has no persistence or concurrency control.

## Broader Domain Design

Planned model components include:

- **`Friendship`** or follower edges with requested, accepted, blocked, and removed states;
- **`Comment`** and per-user **`Reaction`** records rather than aggregate-only likes;
- **`Message`** and conversation membership for private messaging;
- **`NewsFeed`** with a replaceable ranking strategy and pagination;
- privacy/visibility policies for public, friends-only, and custom audiences;
- authentication, profiles, media, stories, timelines, search, notifications, and activity history.

A graph such as `Map<UserId, Set<UserId>>` represents connections. An inverted index supports search, and a priority queue or ranked index can order feed candidates. **Strategy** fits feed ranking and privacy evaluation, **Observer** fits notifications, and **Factory** fits validated content creation; these patterns are extensions and are not implemented in the current classes.

## Implementation Status

The direct-friend text-post demo is implemented. Comments, unlike/remove-friend facade operations, identity-aware reactions, messaging, authentication, privacy, stories/media, search, notifications, durable storage, and ranked feeds are not implemented.

## Run

From `Problems/social_network`:

```bash
javac com/example/lld/social_network/*.java
java com.example.lld.social_network.Main
```
