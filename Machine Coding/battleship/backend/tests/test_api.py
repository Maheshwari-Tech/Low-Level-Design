from fastapi.testclient import TestClient

from app.main import create_app


def test_game_api_is_runnable():
    client = TestClient(create_app())

    state = client.get("/api/game")
    reset = client.post("/api/game/reset")

    assert state.status_code == 200
    assert state.json()["boardSize"] == 8
    assert reset.status_code == 200


def test_rule_violation_has_consistent_error_response():
    client = TestClient(create_app())
    response = client.post(
        "/api/game/ships",
        json={
            "name": "Battleship",
            "row": 0,
            "column": 6,
            "direction": "horizontal",
        },
    )
    assert response.status_code == 400
    assert response.json() == {"detail": "Ship does not fit on the board"}


def test_state_conflict_returns_409():
    client = TestClient(create_app())

    response = client.post("/api/game/shots", json={"row": 0, "column": 0})

    assert response.status_code == 409
    assert response.json() == {"detail": "Place the complete fleet before firing"}


def test_malformed_request_is_rejected_before_domain_execution():
    client = TestClient(create_app())

    response = client.post(
        "/api/game/shots",
        json={"row": -1, "column": 0},
    )

    assert response.status_code == 422
