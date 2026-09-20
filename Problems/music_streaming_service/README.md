# Music Streaming Service

## Problem

Design the domain layer of a Spotify-style catalog, library, playlist, and playback service.

## Required behavior

- Manage artists, albums, tracks, genres, territories, and availability windows.
- Search and browse the catalog without coupling playback to indexing.
- Let users follow artists, save releases, and create ordered playlists.
- Add, remove, and reorder playlist entries with optimistic concurrency control.
- Manage a playback queue with play, pause, seek, skip, repeat, and shuffle commands.
- Record listening events and expose recently played items.
- Enforce subscription and territorial playback policies.

## Core model

`Artist`, `Album`, `Track`, `AudioAsset`, `User`, `Library`, `Playlist`, `PlaylistEntry`, `PlaybackSession`, `Queue`, and `PlaybackPolicy`.

The player consumes authorized media references; it does not own catalog metadata or billing. Shuffle should maintain a deterministic session order so back/forward remains coherent.

## Invariants and edge cases

- Playlist entry identity is independent from track identity, allowing repeats.
- A stale reorder cannot silently overwrite another edit.
- Only one command wins when several clients mutate the same playback session.
- Handle unavailable tracks, deleted catalog items, device handoff, queue exhaustion, and offline events.

## Design exercise

Demonstrate catalog browsing, playlist edits, a stale update, playback controls, deterministic shuffle, device transfer, and territory rejection.
