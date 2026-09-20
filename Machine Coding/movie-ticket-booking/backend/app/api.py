from datetime import datetime
from decimal import Decimal

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from .application import BookingService
from .domain import BookingError, HoldExpiredError, NotFoundError


class HoldRequest(BaseModel):
    seats: list[str] = Field(min_length=1, max_length=6)


class ConfirmRequest(BaseModel):
    hold_id: str
    customer_name: str = Field(min_length=2, max_length=80)


def create_router(service: BookingService) -> APIRouter:
    router = APIRouter(prefix="/api")

    @router.get("/shows")
    def list_shows():
        return [serialize_show(show) for show in service.list_shows()]

    @router.get("/shows/{show_id}/seats")
    def seat_map(show_id: str):
        try:
            return {
                "showId": show_id,
                "seats": [
                    {"id": seat, "status": status}
                    for seat, status in service.seat_map(show_id).items()
                ],
            }
        except NotFoundError as error:
            raise HTTPException(404, str(error)) from error

    @router.post("/shows/{show_id}/holds", status_code=201)
    def hold(show_id: str, request: HoldRequest):
        try:
            result = service.hold_seats(show_id, request.seats)
            return {
                "holdId": result.id,
                "showId": result.show_id,
                "seats": sorted(result.seats),
                "expiresAt": result.expires_at,
            }
        except BookingError as error:
            raise HTTPException(
                404 if isinstance(error, NotFoundError) else 409, str(error)
            ) from error

    @router.post("/shows/{show_id}/bookings", status_code=201)
    def confirm(show_id: str, request: ConfirmRequest):
        try:
            result = service.confirm_booking(
                show_id, request.hold_id, request.customer_name
            )
            return {
                "bookingId": result.id,
                "showId": result.show_id,
                "customerName": result.customer_name,
                "seats": sorted(result.seats),
                "total": result.total,
            }
        except BookingError as error:
            status = (
                410
                if isinstance(error, HoldExpiredError)
                else 404 if isinstance(error, NotFoundError) else 409
            )
            raise HTTPException(status, str(error)) from error

    return router


def serialize_show(show) -> dict[str, str | datetime | Decimal]:
    return {
        "id": show.id,
        "movie": show.movie,
        "screen": show.screen,
        "startsAt": show.starts_at,
        "price": show.price,
    }
