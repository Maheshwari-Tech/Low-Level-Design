from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from threading import RLock

from .domain import Booking, SeatHold, SeatInventory, Show


class InMemoryBookingRepository:
    def __init__(self) -> None:
        seats = frozenset(f"{row}{number}" for row in "ABCDE" for number in range(1, 7))
        shows = (
            Show(
                "show-1",
                "The Silent Orbit",
                "Screen 1",
                datetime.fromisoformat("2026-09-21T19:30:00"),
                Decimal("280"),
                seats,
            ),
            Show(
                "show-2",
                "Paper Planes",
                "Screen 2",
                datetime.fromisoformat("2026-09-21T21:00:00"),
                Decimal("220"),
                seats,
            ),
        )
        self._shows = {show.id: show for show in shows}
        self._holds: dict[str, SeatHold] = {}
        self._bookings: dict[str, Booking] = {}
        self._lock = RLock()

    def list_shows(self) -> tuple[Show, ...]:
        with self._lock:
            return tuple(self._shows.values())

    def get_show(self, show_id: str) -> Show | None:
        with self._lock:
            return self._shows.get(show_id)

    def get_hold(self, hold_id: str) -> SeatHold | None:
        with self._lock:
            return self._holds.get(hold_id)

    def unavailable_seats(self, show_id: str, now: datetime) -> SeatInventory:
        with self._lock:
            self._remove_expired_holds(now)
            held = frozenset(
                seat
                for hold in self._holds.values()
                if hold.show_id == show_id
                for seat in hold.seats
            )
            return SeatInventory(held=held, booked=self._booked_seats(show_id))

    def _booked_seats(self, show_id: str) -> frozenset[str]:
        return frozenset(
            seat
            for booking in self._bookings.values()
            if booking.show_id == show_id
            for seat in booking.seats
        )

    def save_hold_if_available(self, hold: SeatHold, now: datetime) -> bool:
        with self._lock:
            self._remove_expired_holds(now)
            current = self._booked_seats(hold.show_id) | frozenset(
                seat
                for existing in self._holds.values()
                if existing.show_id == hold.show_id
                for seat in existing.seats
            )
            if hold.seats.intersection(current):
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
            self._holds.pop(booking.hold_id, None)
            return True

    def _remove_expired_holds(self, now: datetime) -> None:
        expired_ids = [
            hold_id
            for hold_id, hold in self._holds.items()
            if hold.is_expired(now)
        ]
        for hold_id in expired_ids:
            self._holds.pop(hold_id, None)
