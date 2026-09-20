from fastapi.testclient import TestClient

from app.main import create_app


def test_availability_and_scheduling_through_http_api():
    client = TestClient(create_app())

    availability = client.get(
        "/api/availability",
        params={"day": "2026-09-22", "participants": "ava@demo.com,sam@demo.com"},
    )
    meeting = client.post(
        "/api/meetings",
        json={
            "title": "Architecture discussion",
            "participants": ["ava@demo.com", "sam@demo.com"],
            "start": "2026-09-22T11:00:00",
            "end": "2026-09-22T11:30:00",
        },
    )

    assert availability.status_code == 200
    assert len(availability.json()["slots"]) == 16
    assert meeting.status_code == 201
