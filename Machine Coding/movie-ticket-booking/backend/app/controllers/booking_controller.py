from app.models.booking import Booking
from app.models.seat_hold import SeatHold
from app.models.show import Show
from app.services.booking_service import BookingService


class BookingController:
    def __init__(self, service: BookingService) -> None:
        self._service = service

    def list_shows(self) -> list[dict[str, object]]:
        return [self._serialize_show(show) for show in self._service.list_shows()]

    def get_seat_map(self, show_id: str) -> dict[str, object]:
        seat_map = self._service.seat_map(show_id)
        return {
            "showId": show_id,
            "seats": [
                {"id": seat, "status": status}
                for seat, status in seat_map.items()
            ],
        }

    def hold_seats(self, show_id: str, seats: list[str]) -> dict[str, object]:
        return self._serialize_hold(self._service.hold_seats(show_id, seats))

    def confirm_booking(
        self,
        show_id: str,
        hold_id: str,
        customer_name: str,
    ) -> dict[str, object]:
        booking = self._service.confirm_booking(show_id, hold_id, customer_name)
        return self._serialize_booking(booking)

    @staticmethod
    def _serialize_show(show: Show) -> dict[str, object]:
        return {
            "id": show.id,
            "movie": show.movie,
            "screen": show.screen,
            "startsAt": show.starts_at,
            "price": show.price,
        }

    @staticmethod
    def _serialize_hold(hold: SeatHold) -> dict[str, object]:
        return {
            "holdId": hold.id,
            "showId": hold.show_id,
            "seats": sorted(hold.seats),
            "expiresAt": hold.expires_at,
        }

    @staticmethod
    def _serialize_booking(booking: Booking) -> dict[str, object]:
        return {
            "bookingId": booking.id,
            "showId": booking.show_id,
            "customerName": booking.customer_name,
            "seats": sorted(booking.seats),
            "total": booking.total,
        }
