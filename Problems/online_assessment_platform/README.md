# Online Assessment Platform

## Interview brief

Design an assessment platform while preserving four distinct prompt variations rather than reducing them to “an exam website”:

1. a general GRE/TOEFL-style assessment platform;
2. a quizzing application;
3. an exam portal with negative marking, subject scores, ranks, mock ranks, and a limited pre-exam used to generalize ranks at much larger scale; and
4. a resumable test-taking site with multiple question types, media, per-user question uniqueness, history, rankings, and a server-enforced timer.

## Scope and variations

- Authors create question-bank and test versions with sections and marking policies.
- Assign fixed or generated question sets to eligible candidates.
- Support MCQ, multiple-correct, yes/no, fill-in, and text/image question content.
- Start, autosave, resume, submit, and time out an attempt.
- Calculate total and subject scores, including negative marks.
- Publish rank snapshots for actual, mock, and extrapolated cohorts with their methodology labeled.
- Keep remote proctoring and subjective human grading as explicit extensions.

## Core model

`QuestionBank`, `QuestionVersion`, `Option`, `Test`, `TestVersion`, `Section`, `MarkingPolicy`, `Candidate`, `QuestionAssignment`, `Attempt`, `Answer`, `AttemptTimer`, `Score`, `SubjectScore`, `Cohort`, `RankSnapshot`, and `ResultPublication`.

Test content is immutable after scheduling. An attempt owns one assigned question set and one authoritative deadline, allowing safe resume without generating a different paper.

## Invariants

- Every attempt references one immutable test version, marking policy, and assigned question set.
- An assigned set has no duplicate question and satisfies declared coverage/difficulty constraints.
- “Unique per user” follows an explicit policy; strict cross-candidate uniqueness fails cleanly when the bank cannot supply enough questions.
- Server time determines start and deadline; client clocks never extend the attempt.
- Each answer belongs to an assigned question and is versioned/idempotent.
- Finalization happens once, either by submit or deadline, and freezes scored answers.
- Negative marking uses the snapshotted policy with exact numeric rules.
- A rank is tied to one cohort and rank-computation version; mock/extrapolated rank is never presented as an actual cohort rank.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `publishTest(draftId, expectedVersion)` | Validate and freeze content, timing, and marking rules. |
| `startAttempt(testId, candidateId, idempotencyKey)` | Allocate questions and return authoritative timing. |
| `saveAnswer(attemptId, questionId, answer, sequence)` | Autosave one newer answer revision. |
| `attemptState(attemptId)` | Resume assigned questions, answers, and remaining server time. |
| `submit(attemptId, expectedVersion, idempotencyKey)` | Finalize once before/at the deadline policy. |
| `score(attemptId)` | Produce total and subject breakdown from frozen data. |
| `rank(cohortId, scoringVersion)` | Build an immutable rank snapshot with tie policy. |
| `history(candidateId, cursor)` | Return authorized past attempts and published results. |

## Key flows, concurrency, and failure

Starting an attempt validates eligibility and schedule, chooses questions under coverage constraints, reserves any strict uniqueness keys, stores the assignment/deadline, then returns content. A retry returns the same attempt. Pre-generation can absorb launch spikes while the same uniqueness constraints remain authoritative.

Autosave accepts monotonically increasing client sequence numbers and returns the stored revision. Reconnect loads server state and merges only newer acknowledged edits. A candidate may resume after a network drop only before the server deadline; an expiry worker and a late submit race through one conditional `IN_PROGRESS -> FINALIZED` transition.

Scoring is deterministic from test/answer versions. Ranking is asynchronous after the cohort closes, using a documented tie rule. The limited pre-exam variation produces a separately labeled statistical projection for a larger population; it cannot be represented as a literal rank among millions who did not participate.

Media uses immutable object references and bounded prefetch. A content-delivery failure is distinguishable from unanswered work and follows the declared incident policy.

## Design decisions

- Snapshot every rule needed to reproduce a result.
- Separate attempt ingestion, scoring, and ranking so launch traffic does not block result computation.
- Use typed answer validators/scorers per question type.
- Keep assignment strategy pluggable: fixed paper, randomized, adaptive, or cohort-balanced.

## Follow-up questions

- Is cross-candidate question uniqueness strict, best-effort, or only within one attempt?
- What test scale, launch burst, timer tolerance, and autosave latency are required?
- How are partial credit, negative marking, ties, appeals, and regrading handled?
- Are offline attempts, proctoring, plagiarism checks, or accessibility accommodations needed?
- How must mock/extrapolated rank confidence and methodology be disclosed?

## Provenance and implementation status

- Pinned prompts: [general online assessment](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L140), [quizzing app](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L175), [scoring/ranking/mock/pre-exam portal](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L199-L203), and [resumable multi-type test site](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L236-L239).
- The pinned primer has no solution-index row or local implementation for these topics.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
