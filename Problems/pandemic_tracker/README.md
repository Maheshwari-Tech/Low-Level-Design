# Pandemic Tracker

## Prompt and scope

Design an in-memory tracker for people, diseases, locations, and outcomes. Record infection status and provide worldwide totals, per-disease summaries, and country/state breakdowns. The source also names cure updates and weekly/monthly/yearly trends, but those paths are unfinished.

Clinical diagnosis, contact tracing, hospital capacity, vaccination, and public dashboards are follow-ups.

## Core model

- `Person`: identity, location, and disease occurrences.
- `Disease`: disease name, infection date, and outcome status.
- `Location`: country, state, and district hierarchy.
- `Repository`: source in-memory patient collection.
- `TrackerService`: world, disease, country, and state aggregation plus update stubs.
- `TrendAnalysis`: named time-bucket analysis extension point.

For a robust model, represent a patient's infection as a distinct `Case` with its own ID, disease, dates, location snapshot, and status history rather than sharing mutable `Disease` objects.

## Invariants

- Person IDs and case IDs are unique; the same case is not inserted twice.
- Status is a closed enum such as `ACTIVE`, `CURED`, or `DEAD`, never an arbitrary string.
- A case has one disease and one location snapshot at a point in time.
- Status transitions are valid, timestamped, and idempotent; aggregate totals equal the cases in the selected snapshot.
- Country/state/district values are normalized identifiers, not case-sensitive free text.

## API

The source exposes reporting methods named `ShowWorldSummary`, `ShowWorldSummaryDiseasesBreakup`, `ShowCountryBreakup`, and `ShowStateBreakup`, plus empty `cured` and trend methods. A clearer boundary is:

```text
recordCase(person, disease, location, infectedAt, commandId) -> CaseId
changeStatus(caseId, status, occurredAt, commandId)
summary(scope, asOf) -> CaseSummary
breakdown(scope, dimension, asOf) -> List<BucketSummary>
trend(scope, interval, from, to) -> TimeSeries
```

## Main flow

1. Normalize identities, disease, and location; reject duplicate commands.
2. Create or update a case and append a status event.
3. Commit the case/event atomically.
4. Query a consistent snapshot and group cases by disease or geographic dimension.
5. Derive trends from event timestamps rather than from current status alone.

## Concurrency and failure handling

- Concurrent case updates need optimistic versions or per-case serialization.
- Reports must define snapshot consistency while writes continue; incremental counters require transactional updates or rebuildable events.
- Reject missing cases, invalid transitions, future/ill-ordered dates, malformed locations, and unknown dimensions.
- Protect sensitive health data with authorization, aggregation thresholds, audit, retention, and deletion policy.

## Design solution

Use `Case` as the aggregate and an append-only `CaseStatusChanged` history as the source for both current state and trends. Keep repositories behind interfaces and make reporting a separate read service. For small in-memory data, group immutable snapshots on demand; at scale, project events into counters keyed by time bucket, disease, and geographic scope.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Partial extracted implementation | [`References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pendamictracker`](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pendamictracker/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/pendamictracker) | Misspelled source path; seeded repository and summary implementation. |
| **Exact duplicate archive** | [`References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pandemictracker.zip`](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pandemictracker.zip) | [ZIP at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/pandemictracker.zip) | Archive copy of the extracted implementation; not a second solution. |

## Follow-ups

- Add recovery/fatality transition rules and corrections with audit history.
- Support district and date-range filters, moving averages, and comparable rates per population.
- Design privacy-preserving public aggregates and restricted case-level access.
- Rebuild projections from events and reconcile late/out-of-order reports.

## Implementation status

**Runnable but partial reference; no code copied here.** The source compiles with unchecked-operation warnings and its seeded `App` prints world, disease, country, and state summaries. Cure and trend operations are empty. Seed data reuses one mutable disease list across people, inserts one person twice, compares status strings with `==`, and the unused `TrendAnalysis` recursively constructs itself.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
