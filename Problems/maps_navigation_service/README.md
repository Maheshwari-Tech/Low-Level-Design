# Maps Navigation Service

## Interview brief

Design a navigation client that builds a path from point A to point B for walking, car, bus, or bike. Preserve the separate source variation for navigation alerts: route construction and live guidance are related but different responsibilities.

## Scope and variations

- Resolve origin/destination and request route alternatives by transport mode.
- Represent a route as legs and user-facing maneuvers.
- Encapsulate walking, driving, cycling, and transit cost/constraint policies.
- Start a guidance session, consume location updates, detect off-route movement, and reroute.
- Deliver relevant navigation alerts for incidents, closures, hazards, and maneuvers.
- Keep global map ingestion, turn-by-turn voice rendering, and autonomous-driving decisions outside the baseline.

## Core model

`Coordinate`, `Place`, `RoadGraph`, `Vertex`, `Edge`, `TransportMode`, `RoutingPolicy`, `Route`, `Leg`, `Maneuver`, `GuidanceSession`, `LocationFix`, `NavigationAlert`, and `TrafficSnapshot`.

Routing policies define edge eligibility and cost. Guidance owns session progress against one immutable route revision and may replace it with a reroute.

## Invariants

- Every route begins and ends at its requested snapped locations.
- Every consecutive edge is connected and permitted for the selected transport mode.
- Route cost and ETA identify the graph/traffic versions used to compute them.
- A transit leg respects service calendars and transfer constraints.
- A guidance session has one active route revision at a time.
- An alert is emitted only when relevant to route, direction, distance/time window, and freshness.
- Older location, reroute, or alert updates cannot regress a newer session revision.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `routes(origin, destination, mode, preferences)` | Return ranked route alternatives and calculation version. |
| `startGuidance(routeId, userContext)` | Create a navigation session. |
| `updateLocation(sessionId, fix, sequence)` | Advance progress, maneuvers, and reroute decisions. |
| `alerts(sessionId, afterCursor)` | Read ordered relevant guidance/incident alerts. |
| `reportIncident(location, type, evidence)` | Submit a moderated map event. |
| `endGuidance(sessionId, reason)` | Close the session idempotently. |

## Key flows, concurrency, and failure

Route calculation snaps endpoints, selects a mode strategy, searches the versioned graph, applies current traffic/transit costs, builds maneuvers, and ranks alternatives. A timeout can return a partial/best-known alternative only if the API labels it clearly.

Location updates carry monotonic sequence/time. The matcher projects noisy fixes onto route geometry, advances progress, and emits a maneuver once per session/revision. Repeated off-route evidence triggers a reroute; one in-flight reroute owns the session version so stale results are discarded.

Incident ingestion deduplicates evidence and publishes map-version changes. Alerts are filtered near the active corridor and expire. Offline clients cache bounded graph/route data, surface staleness, and reconcile once connected.

## Design decisions

- Use Strategy for mode-specific eligibility/cost, not a switch spread across the client.
- Keep graph data and routing engine behind ports so local/offline and remote implementations can coexist.
- Treat ETA as a versioned estimate, not an invariant.
- Separate incident alerts from deterministic maneuver instructions, even when delivered through one stream.

## Follow-up questions

- Is routing local, server-side, or hybrid/offline?
- Are public transit schedules, live traffic, tolls, accessibility, and multi-stop routes required?
- What location accuracy and reroute latency are acceptable?
- How are incident reports trusted, merged, and expired?
- What user-location retention and privacy constraints apply?

## Provenance and implementation status

- Pinned prompts: [multimodal maps navigator](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L89-L92) and [navigation alerts](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L210).
- Related primer index row: [Maps Navigator Client OOD](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/solutions.md#L24). It links externally; the primer itself provides no local implementation.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.
