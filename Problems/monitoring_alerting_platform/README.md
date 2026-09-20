# Monitoring and Alerting Platform

## Interview brief

Design monitoring and alerting for production and non-production environments. Preserve the source's concrete certificate-health dashboard and its adjacent HTTP scheduling variations instead of flattening them into one vague “monitoring system.”

## Scope and variations

- Ingest metrics and structured health events from services, hosts, and assets.
- Evaluate threshold, absence, and windowed rules by environment and tenant.
- Open, acknowledge, resolve, silence, and route alert instances.
- Display machines with valid, expiring, or invalid certificates.
- Poll a passed endpoint on a fixed interval when push telemetry is unavailable.
- Keep a general bounded-concurrency HTTP job scheduler as a separate execution component; monitoring may use it but does not own arbitrary business jobs.

## Core model

`Environment`, `MonitoredResource`, `MetricSeries`, `Sample`, `HealthEvent`, `CertificateObservation`, `AlertRule`, `EvaluationWindow`, `AlertInstance`, `Incident`, `NotificationRoute`, `Silence`, `PollSchedule`, and `PollRun`.

Rules are versioned policy. Alert instances are stateful outcomes keyed by rule plus resource/dimensions so repeated evaluations update one incident rather than creating a storm.

## Invariants

- Samples are associated with a tenant, environment, resource, metric, event time, and ingestion time.
- One active alert instance exists per rule and deduplication key.
- A rule version evaluates a declared window; late data never silently rewrites a closed incident without an explicit correction policy.
- A silence suppresses notification, not state evaluation or audit history.
- Certificate status derives from parsed validity, chain/trust policy, hostname, and evaluation time; “good” is not just `notAfter > now`.
- A polling schedule has at most one active run per configured overlap policy.
- Acknowledgement and resolution transitions are version-checked and auditable.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `ingest(samples, sourceId, idempotencyKey)` | Accept bounded telemetry batches. |
| `upsertRule(rule, expectedVersion)` | Version alert policy and routing. |
| `querySeries(selector, range, resolution)` | Read downsampled or raw observations. |
| `listAlerts(filters, cursor)` | Power dashboards and operational triage. |
| `acknowledge(alertId, actor, expectedVersion)` | Record ownership without stopping evaluation. |
| `createSilence(matchers, window, reason)` | Suppress matching notifications temporarily. |
| `upsertPollSchedule(endpoint, interval, overlapPolicy)` | Configure the monitoring-specific fixed-interval collector. |
| `certificateFleet(filters)` | Return valid, expiring, and invalid machine certificates. |

## Key flows, concurrency, and failure

Ingestion validates cardinality and timestamps, writes observations, and publishes evaluation work. An evaluator reads the rule's window, computes a state, then compare-and-sets the keyed alert instance. State transitions create an outbox event; routing, grouping, retry, and escalation occur asynchronously.

For certificate monitoring, a collector records the observed chain and handshake evidence. Evaluation applies hostname, trust, revocation/availability policy, and configurable expiry thresholds. The dashboard reads materialized fleet status but links back to the source observation.

The polling runner leases due schedules, executes with timeouts and SSRF controls, records one run, and calculates the next due time. “Ten scheduling requests at a time, 100 jobs at a time” is a capacity-control variant: admission and execution limits are separate semaphores/queues, not magic thread counts.

Duplicate samples, evaluator retries, and notification retries are idempotent. Missing telemetry is distinguishable from a healthy zero. High-cardinality labels, clock skew, delayed data, flapping alerts, and downstream paging outages require explicit limits and backoff.

## Design decisions

- Separate collection, storage, evaluation, incident state, and notification routing.
- Use event time for windows and ingestion time for lateness/operational diagnostics.
- Apply hysteresis, minimum duration, and grouped notifications to reduce flapping and alert storms.
- Partition by tenant/resource hash while keeping one deduplication owner for an alert key.

## Follow-up questions

- Which telemetry types, retention periods, query latency, and cardinality limits apply?
- Are rules evaluated continuously, periodically, or on every new sample?
- What are the escalation, on-call, maintenance-window, and multi-region requirements?
- How should stale/missing data affect health?
- Is endpoint polling trusted-only, and what network destinations are permitted?

## Provenance and implementation status

- Pinned prompts: [environment monitoring and alerting](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L144), [certificate-health analytics dashboard](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L268), [bounded-concurrency HTTP scheduler](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L265), and [fixed-interval endpoint caller](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L269).
- The pinned primer has no solution-index row or local implementation for these topics.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
