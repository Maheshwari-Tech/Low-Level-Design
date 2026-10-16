from datetime import datetime
from decimal import Decimal
from threading import Lock

from app.models.booking import Booking
from app.models.seat_hold import SeatHold
from app.models.seat_inventory import SeatInventory
from app.models.show import Show


class InMemoryBookingRepository:
    def __init__(self, shows: tuple[Show, ...] | None = None) -> None:
        configured_shows = shows if shows is not None else _default_shows()
        self._shows = {show.id: show for show in configured_shows}
        self._holds: dict[str, SeatHold] = {}
        self._bookings: dict[str, Booking] = {}
        self._lock = Lock()

    def list_shows(self) -> tuple[Show, ...]:
        with self._lock:
            return tuple(self._shows.values())

    def get_show(self, show_id: str) -> Show | None:
        with self._lock:
            return self._shows.get(show_id)

    def get_hold(self, hold_id: str) -> SeatHold | None:
        with self._lock:
            return self._holds.get(hold_id)

    def get_seat_inventory(self, show_id: str, now: datetime) -> SeatInventory:
        with self._lock:
            self._remove_expired_holds(now)
            return SeatInventory(
                held_seats=self._held_seats(show_id),
                booked_seats=self._booked_seats(show_id),
            )

    def save_hold_if_available(self, hold: SeatHold, now: datetime) -> bool:
        with self._lock:
            self._remove_expired_holds(now)
            unavailable_seats = self._held_seats(hold.show_id) | self._booked_seats(
                hold.show_id
            )
            if hold.seats.intersection(unavailable_seats):
                return False
            self._holds[hold.id] = hold
            return True

    def save_booking_if_hold_active(self, booking: Booking, now: datetime) -> bool:
        with self._lock:
            hold = self._holds.get(booking.hold_id)
            if (
                hold is None
                or hold.is_expired(now)
                or hold.show_id != booking.show_id
                or hold.seats != booking.seats
                or booking.seats.intersection(self._booked_seats(booking.show_id))
            ):
                return False

            self._bookings[booking.id] = booking
            self._holds.pop(booking.hold_id)
            return True

    def _booked_seats(self, show_id: str) -> frozenset[str]:
        return frozenset(
            seat
            for booking in self._bookings.values()
            if booking.show_id == show_id
            for seat in booking.seats
        )

    def _held_seats(self, show_id: str) -> frozenset[str]:
        return frozenset(
            seat
            for hold in self._holds.values()
            if hold.show_id == show_id
            for seat in hold.seats
        )

    def _remove_expired_holds(self, now: datetime) -> None:
        expired_hold_ids = [
            hold_id
            for hold_id, hold in self._holds.items()
            if hold.is_expired(now)
        ]
        for hold_id in expired_hold_ids:
            self._holds.pop(hold_id)


def _default_shows() -> tuple[Show, ...]:
    seats = frozenset(f"{row}{number}" for row in "ABCDE" for number in range(1, 7))
    return (
        Show(
            id="show-1",
            movie="The Silent Orbit",
            screen="Screen 1",
            starts_at=datetime.fromisoformat("2026-09-21T19:30:00"),
            price=Decimal("280"),
            seats=seats,
        ),
        Show(
            id="show-2",
            movie="Paper Planes",
            screen="Screen 2",
            starts_at=datetime.fromisoformat("2026-09-21T21:00:00"),
            price=Decimal("220"),
            seats=seats,
        ),
    )
