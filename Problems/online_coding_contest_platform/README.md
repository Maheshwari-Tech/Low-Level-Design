# Online Coding Contest Platform

## Interview Prompt

Design a platform where authors publish coding questions, organizers run contests, participants submit answers, and the system calculates scores and a deterministic leaderboard.

## Scope and Requirements

1. Register users with participant, author, and organizer capabilities.
2. Create versioned questions with difficulty, tags, score, test cases, and visibility.
3. Schedule a contest, select questions, and enroll eligible participants.
4. Accept a submission only while the contest is active and queue it for judging.
5. Record per-test results, total score, penalty, and final verdict idempotently.
6. Publish a deterministic leaderboard and a user's contest history.

The first design treats sandbox execution as an external `JudgePort`; building a secure container runtime is a separate system-design exercise.

## Core Model

| Type | Responsibility |
| --- | --- |
| `Question` / `QuestionVersion` | Stable identity plus immutable judged content |
| `Contest` | Aggregate for schedule, roster, question snapshots, and rules |
| `Submission` | Immutable source attempt and asynchronous judging state |
| `Judgement` | Test outcomes, score, penalty, and verdict |
| `ScorePolicy` | Converts accepted attempts and time into rank data |
| `LeaderboardEntry` | Materialized best score and tie-break keys |
| `JudgePort` | Executes a submission in an isolated external worker |
| `ContestRepository` | Versioned lifecycle and enrollment persistence |

## Invariants

- A contest question points to an immutable question version.
- A submission is accepted only inside the contest interval for an enrolled user.
- One judge callback finalizes a submission at most once.
- A participant's score uses the configured best/latest attempt rule consistently.
- Leaderboard ties use documented keys such as score, penalty, last accepted time, then user ID.

## API Sketch

```java
ContestId createContest(CreateContest command);
Contest publish(ContestId id, long expectedVersion);
void enroll(ContestId contestId, UserId participant);
SubmissionId submit(SubmitCode command, String idempotencyKey);
void recordJudgement(SubmissionId id, Judgement result, String callbackId);
LeaderboardPage leaderboard(ContestId id, PageCursor cursor);
```

## Flow, Concurrency, and Failure

The submit transaction validates the contest clock, snapshots language/source metadata, stores an idempotency record, and writes a `SubmissionQueued` outbox event. Workers call the sandbox adapter and post a callback. Compare-and-set changes `QUEUED/RUNNING` to a terminal verdict; stale callbacks are ignored. Update a leaderboard projection from terminal judgements, but retain submissions as the source of truth so rankings can be rebuilt after a policy correction.

## Source-Backed Solution Variation

- `References/kumaransg-LLD/leetcode-lld-flipkart-coding-blox/` — the actual Spring Data JPA/Maven project covering users, questions, contests, scoring, leaderboard, and history. [Pinned upstream source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/leetcode-lld-flipkart-coding-blox)

## Interview Follow-Ups

- Plagiarism detection and hidden test-case security.
- Multiple languages and compiler versions.
- Freeze/unfreeze leaderboards.
- Team contests and partial scoring.
- Judge retries, poison submissions, and resource quotas.

## Implementation Status

This canonical page supplies a complete design walkthrough. The source project is preserved unmodified in the local clone and may require its original framework/dependency environment to run.
