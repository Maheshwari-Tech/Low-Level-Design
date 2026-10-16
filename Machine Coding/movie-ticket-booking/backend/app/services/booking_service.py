from collections.abc import Callable
from datetime import datetime, timedelta, timezone
from uuid import uuid4

from app.exceptions.booking_exceptions import (
    HoldExpiredError,
    HoldNotFoundError,
    SeatUnavailableError,
    ShowNotFoundError,
)
from app.models.booking import Booking
from app.models.enums import SeatStatus
from app.models.seat_hold import SeatHold
from app.models.show import Show
from app.repositories.booking_repository import BookingRepository

Clock = Callable[[], datetime]
IdGenerator = Callable[[], str]


class BookingService:
    def __init__(
        self,
        repository: BookingRepository,
        clock: Clock | None = None,
        hold_duration: timedelta = timedelta(minutes=5),
        hold_id_generator: IdGenerator | None = None,
        booking_id_generator: IdGenerator | None = None,
    ) -> None:
        if hold_duration <= timedelta(0):
            raise ValueError("Hold duration must be positive")

        self._repository = repository
        self._clock = clock or (lambda: datetime.now(timezone.utc))
        self._hold_duration = hold_duration
        self._hold_id_generator = hold_id_generator or (lambda: str(uuid4()))
        self._booking_id_generator = booking_id_generator or (
            lambda: f"BK-{uuid4().hex[:8].upper()}"
        )

    def list_shows(self) -> tuple[Show, ...]:
        return self._repository.list_shows()

    def seat_map(self, show_id: str) -> dict[str, SeatStatus]:
        show = self._get_show(show_id)
        inventory = self._repository.get_seat_inventory(show_id, self._clock())
        return {seat: inventory.status_of(seat) for seat in sorted(show.seats)}

    def hold_seats(self, show_id: str, seats: list[str]) -> SeatHold:
        show = self._get_show(show_id)
        requested_seats = self._validate_seats(show, seats)
        now = self._clock()
        hold = SeatHold(
            id=self._hold_id_generator(),
            show_id=show_id,
            seats=requested_seats,
            expires_at=now + self._hold_duration,
        )
        if not self._repository.save_hold_if_available(hold, now):
            raise SeatUnavailableError("One or more seats are no longer available")
        return hold

    def confirm_booking(
        self,
        show_id: str,
        hold_id: str,
        customer_name: str,
    ) -> Booking:
        show = self._get_show(show_id)
        hold = self._repository.get_hold(hold_id)
        if hold is None or hold.show_id != show_id:
            raise HoldNotFoundError("Hold not found")
        if hold.is_expired(self._clock()):
            raise HoldExpiredError("Hold has expired")

        booking = Booking(
            id=self._booking_id_generator(),
            show_id=show_id,
            hold_id=hold.id,
            customer_name=customer_name,
            seats=hold.seats,
            total=show.price * len(hold.seats),
        )
        if not self._repository.save_booking_if_hold_active(booking, self._clock()):
            raise HoldExpiredError("Hold is no longer active")
        return booking

    def _get_show(self, show_id: str) -> Show:
        show = self._repository.get_show(show_id)
        if show is None:
            raise ShowNotFoundError("Show not found")
        return show

    @staticmethod
    def _validate_seats(show: Show, seats: list[str]) -> frozenset[str]:
        normalized_seats = [seat.strip().upper() for seat in seats if seat.strip()]
        requested_seats = frozenset(normalized_seats)
        if (
            not requested_seats
            or len(normalized_seats) != len(seats)
            or len(requested_seats) != len(normalized_seats)
            or not requested_seats.issubset(show.seats)
        ):
            raise SeatUnavailableError("Seats must be valid, distinct, and non-empty")
        return requested_seats
