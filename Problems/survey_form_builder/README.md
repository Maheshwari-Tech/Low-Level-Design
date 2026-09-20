# Survey and Form Builder

## Interview brief

Design a Google Forms/SurveyMonkey-style system in which authors build and publish versioned forms, respondents submit validated answers, and owners analyze/export results. Clarify whether the emphasis is the form-domain model, collaborative authoring, public response scale, or analytics.

## Scope and variations

- Create drafts containing sections and typed questions.
- Reorder, duplicate, validate, preview, publish, and close a form.
- Support text, number, date, single-choice, multi-choice, rating, and file-reference questions.
- Model required answers, validation rules, conditional navigation, anonymous/authenticated access, and one-response policies.
- Save progress and submit responses against the exact published form version.
- Aggregate results and export responses without mutating raw submissions.

## Core model

`Form`, `FormVersion`, `Section`, `Question`, `QuestionType`, `Option`, `ValidationRule`, `BranchRule`, `AudiencePolicy`, `Response`, `Answer`, `Respondent`, and `ResultProjection`.

Draft editing and response collection use different aggregates. Publishing freezes a `FormVersion`; later edits create a new version rather than changing the schema under existing responses.

## Invariants

- Every response references one immutable published form version.
- An answer belongs to a question in that version and satisfies its type and validation rules.
- Required reachable questions must be answered before submission.
- Branching cannot produce an infinite navigation loop.
- A submitted response is immutable; correction is an explicit revision or replacement policy.
- One-response enforcement uses the declared identity/fingerprint policy and is atomic.
- Anonymous forms do not accidentally persist respondent identity in response records or analytics.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `createForm(owner, settings)` | Create an editable draft. |
| `applyDraftCommand(formId, expectedVersion, command)` | Add/reorder/edit schema with optimistic concurrency. |
| `publish(formId, expectedVersion)` | Validate and freeze a response-ready version. |
| `startResponse(formVersionId, respondentContext)` | Create an allowed in-progress response. |
| `saveAnswers(responseId, answers, expectedVersion)` | Validate and persist resumable progress. |
| `submit(responseId, expectedVersion, idempotencyKey)` | Atomically finalize once. |
| `results(formId, filters, cursor)` | Read authorized aggregates or raw-response pages. |

## Key flows, concurrency, and failure

Publishing validates unique stable question IDs, option references, branch targets, reachability, and cycle policy, then writes an immutable version. Concurrent editors use optimistic versions; a rejected stale command is rebased or resolved deliberately.

Starting a response snapshots audience policy and version. Autosave is versioned and idempotent. Submission revalidates the full reachable path, enforces response limits with a unique key, records `SUBMITTED`, and emits an analytics event through an outbox.

Analytics projections can lag while the submitted response remains authoritative. File uploads should use pre-signed object references and malware scanning; a pending/failed file cannot satisfy a required answer silently.

## Design decisions

- Use typed question strategies instead of a nullable “all possible fields” question class.
- Keep stable question IDs across safe draft edits so analytics can compare versions deliberately.
- Compile branch rules to a validated graph at publish time.
- Separate raw response access from aggregate result permissions.

## Follow-up questions

- Which question types, branching semantics, quotas, and collaboration features are required?
- Can a respondent edit after submission or submit multiple times?
- What anonymity, data-retention, residency, and export requirements apply?
- Must forms work offline and merge later?
- How are spam, abuse, file uploads, and public traffic spikes controlled?

## Provenance and implementation status

- Pinned prompt: [survey similar to Google Forms / SurveyMonkey](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L62).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
