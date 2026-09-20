# Jukebox

## Interview Prompt

Design a physical or venue jukebox that manages a local song catalog, accepts selections, maintains a play queue, and controls a playback device. This device/session problem is distinct from the distributed [Music Streaming Service](../music_streaming_service/README.md).

## Scope and Requirements

1. Browse local songs/albums/artists and enqueue an available track.
2. Apply a credit/payment policy before accepting a paid selection.
3. Preserve queue order and support a configurable fairness policy.
4. Start, pause, resume, skip, and finish playback through a device adapter.
5. Recover queue and credit state after a process restart.
6. Record play history and typed device failures.

## Core Model

| Type | Responsibility |
| --- | --- |
| `Song` / `Album` / `Artist` | Immutable catalog metadata |
| `JukeboxSession` | Aggregate for credits, queue, and playback state |
| `Selection` | User/song/price/idempotency fact |
| `PlayQueue` | Ordered selections plus fairness policy |
| `PlaybackState` | `IDLE`, `PLAYING`, `PAUSED`, `FAULTED` |
| `PlaybackPort` | Adapter for the physical or software player |
| `CreditPolicy` | Prices selections and consumes/refunds credits |

## Invariants and API

- A selection consumes credit exactly once and is enqueued at most once.
- At most one song is playing for a jukebox.
- A failed enqueue does not consume credit; a post-enqueue device fault follows an explicit refund policy.
- Queue mutation and current-playback transition are serialized per device.

```java
SelectionReceipt select(UserId user, SongId song, String idempotencyKey);
PlaybackSnapshot playNext();
PlaybackSnapshot pause();
PlaybackSnapshot resume();
PlaybackSnapshot skip(SkipReason reason);
QueueSnapshot queue();
```

## Design Solution

Use a state machine for playback and Strategy for queue fairness/credit rules. Persist selection acceptance, credit movement, and queue position atomically. Invoke the playback adapter after selecting the next item; if the outcome is unknown, keep a recoverable `STARTING` attempt rather than advancing twice. Device callbacks are idempotent by playback attempt ID.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Design_Problems/SystemDesign/JukeBox/` — Java CD player, playlist, song, artist, and user model. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/SystemDesign/JukeBox)

## Follow-Ups

Priority credits, per-user fairness, explicit-content policy, remote administration, disconnected payment, device health, and crossfade/preloading.

## Implementation Status

The canonical page provides the complete interview design. The actual upstream Java variation stays unchanged in the local clone.
