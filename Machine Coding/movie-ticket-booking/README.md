# Movie Ticket Booking

Independent machine-coding solution focused on inventory consistency and a two-step hold/confirm workflow.

Read [PROBLEM.md](PROBLEM.md) first, followed by [DESIGN.md](DESIGN.md).

## Run

```bash
./run_backend.sh   # http://127.0.0.1:8102/docs
./run_frontend.sh  # http://127.0.0.1:5102
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
| GET | `/api/shows` | List shows |
| GET | `/api/shows/{show_id}/seats` | Read seat availability |
| POST | `/api/shows/{show_id}/holds` | Atomically hold seats |
| POST | `/api/shows/{show_id}/bookings` | Confirm a valid hold |
