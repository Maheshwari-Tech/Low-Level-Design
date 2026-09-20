# Python Machine Coding Reference Solutions

This workspace contains exactly three independent projects. Each project owns its backend, frontend, dependencies, run scripts, problem statement, design, and tests.

| Project | Problem | Backend | Frontend |
|---|---|---:|---:|
| [Battleship](battleship/README.md) | Ship placement and attack rules | 8101 | 5101 |
| [Movie Ticket Booking](movie-ticket-booking/README.md) | Seat holds and booking consistency | 8102 | 5102 |
| [Calendar Meet Booking](calendar-meet-booking/README.md) | Availability and conflict detection | 8103 | 5103 |

Start with [the machine-coding expectations and evaluation rubric](MACHINE_CODING_GUIDE.md), then open a project’s `PROBLEM.md` before reading its solution.

For a staff-level review, use the [engineering review and production-readiness assessment](ENGINEERING_REVIEW.md). It distinguishes what is complete for a 90-minute exercise from what a real deployment still requires.

## Shared architecture rule

All three backends use the same dependency direction:

```text
HTTP adapter → application use cases → domain
                         ↓
                  repository port
                         ↑
               in-memory adapter
```

The domain never imports FastAPI or persistence code. `main.py` is the composition root that wires concrete adapters into the use cases.

## What “production-ready” means here

The code is deliberately runnable in memory for an interview. “Production-ready” refers to explicit invariants, atomic process-local behavior, deterministic test seams, error handling, and a documented migration path. It does **not** claim that in-memory storage, a single-process lock, open CORS, or unauthenticated demo APIs are suitable for deployment.

Each `DESIGN.md` covers:

- domain boundaries and invariants;
- patterns and why they exist;
- concurrency guarantees;
- time and space complexity;
- extension points and deliberate exclusions;
- the database and operational design needed for production.
