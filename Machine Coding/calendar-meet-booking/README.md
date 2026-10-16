# Calendar Meet Booking

Independent machine-coding solution focused on interval modeling and participant conflict detection.

Read [PROBLEM.md](PROBLEM.md) first, followed by [DESIGN.md](DESIGN.md).

## Backend structure

```text
backend/app/
├── api/             # Routes, request schemas, error-to-HTTP mapping
├── controllers/     # Public response mapping
├── exceptions/      # Scheduling failures
├── models/          # Meeting, participant, time range, slots, policy
├── repositories/    # Storage contract and in-memory implementation
├── services/        # Calendar use cases
└── main.py          # Dependency composition
```

## Run

```bash
./run_backend.sh   # http://127.0.0.1:8103/docs
./run_frontend.sh  # http://127.0.0.1:5103
```

Use separate terminals. To run tests:

```bash
cd backend
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

## APIs

| Method | Path | Use case |
|---|---|---|
| GET | `/api/meetings` | List meetings |
| GET | `/api/availability` | Find participant availability |
| POST | `/api/meetings` | Atomically schedule a meeting |
