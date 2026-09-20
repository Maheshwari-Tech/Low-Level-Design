from fastapi.testclient import TestClient

from app.main import create_app


def test_booking_flow_through_http_api():
    client = TestClient(create_app())

    shows = client.get("/api/shows")
    hold = client.post("/api/shows/show-1/holds", json={"seats": ["A1"]})
    booking = client.post(
        "/api/shows/show-1/bookings",
        json={"hold_id": hold.json()["holdId"], "customer_name": "Ada Lovelace"},
    )

    assert shows.status_code == 200
    assert hold.status_code == 201
    assert booking.status_code == 201
    assert booking.json()["seats"] == ["A1"]
