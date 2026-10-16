from fastapi import APIRouter

from app.api.schemas import ConfirmBookingRequest, HoldSeatsRequest
from app.controllers.booking_controller import BookingController


def create_router(controller: BookingController) -> APIRouter:
    router = APIRouter(prefix="/api", tags=["booking"])

    @router.get("/shows")
    def list_shows() -> list[dict[str, object]]:
        return controller.list_shows()

    @router.get("/shows/{show_id}/seats")
    def get_seat_map(show_id: str) -> dict[str, object]:
        return controller.get_seat_map(show_id)

    @router.post("/shows/{show_id}/holds", status_code=201)
    def hold_seats(
        show_id: str,
        request: HoldSeatsRequest,
    ) -> dict[str, object]:
        return controller.hold_seats(show_id, request.seats)

    @router.post("/shows/{show_id}/bookings", status_code=201)
    def confirm_booking(
        show_id: str,
        request: ConfirmBookingRequest,
    ) -> dict[str, object]:
        return controller.confirm_booking(
            show_id,
            request.hold_id,
            request.customer_name,
        )

    return router
