# Battleship

Python, FastAPI, and React reference solution for a 90-minute machine-coding
round.

Start with the [problem statement](PROBLEM.md), then use these documents during
the design discussion:

- [Architecture and object model](DESIGN.md)
- [Design decisions and alternatives](DESIGN_DECISIONS.md)
- [Production-readiness plan](PRODUCTION_READINESS.md)

## Backend structure

```text
backend/app/
├── api/            # FastAPI routes, schemas, error mapping
├── controllers/    # Request/response orchestration
├── exceptions/     # Domain-specific failures
├── factories/      # Ship construction
├── models/         # Board, game, ship, cell, shot
├── repositories/   # Repository contract and in-memory adapter
├── services/       # Application use cases
└── main.py         # Dependency composition
```

## Run

```bash
./run_backend.sh   # API: http://127.0.0.1:8101/docs
./run_frontend.sh  # UI:  http://127.0.0.1:5101
```

Run each command in a separate terminal.

To run tests:

```bash
cd backend
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

The backend script creates `backend/.venv` and installs runtime dependencies on
first use. The frontend script installs npm dependencies on first use.

## APIs

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/game` | Read current game state |
| POST | `/api/game/ships` | Place a ship |
| POST | `/api/game/shots` | Fire at a cell |
| POST | `/api/game/reset` | Start over |

Interactive API documentation is available at
`http://127.0.0.1:8101/docs` while the backend is running.

## Suggested 90-minute approach

| Time | Outcome |
|---|---|
| 0–10 min | Clarify coordinates, fleet, lifecycle, invalid operations, and scope. |
| 10–30 min | Implement `Cell`, `Ship`, `Board`, and `BattleshipGame` with invariants. |
| 30–50 min | Add service, repository boundary, controller, and HTTP endpoints. |
| 50–65 min | Cover placement, firing, sinking, winning, and invalid transitions with tests. |
| 65–80 min | Add the small UI and run the main flow end to end. |
| 80–90 min | Document trade-offs, known limits, and production evolution. |

The order is deliberate: a complete, tested domain is more valuable than a
large UI or speculative infrastructure.
