from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from enum import StrEnum


class BookingError(ValueError):
    """Base error for an invalid booking command."""


class NotFoundError(BookingError):
    pass


class SeatUnavailableError(BookingError):
    pass


class HoldExpiredError(BookingError):
    pass


class InvalidBookingError(BookingError):
    pass


class SeatStatus(StrEnum):
    AVAILABLE = "available"
    HELD = "held"
    BOOKED = "booked"


@dataclass(frozen=True)
class SeatInventory:
    """One atomic view of seats unavailable for a show."""

    held: frozenset[str]
    booked: frozenset[str]

    def status_of(self, seat: str) -> SeatStatus:
        if seat in self.booked:
            return SeatStatus.BOOKED
        if seat in self.held:
            return SeatStatus.HELD
        return SeatStatus.AVAILABLE


@dataclass(frozen=True)
class Show:
    id: str
    movie: str
    screen: str
    starts_at: datetime
    price: Decimal
    seats: frozenset[str]

    def __post_init__(self) -> None:
        if not self.id.strip() or not self.movie.strip() or not self.screen.strip():
            raise InvalidBookingError("Show id, movie, and screen are required")
        if self.price <= Decimal("0"):
            raise InvalidBookingError("Show price must be positive")
        normalized_seats = frozenset(
            seat.strip().upper() for seat in self.seats if seat.strip()
        )
        if not normalized_seats:
            raise InvalidBookingError("Show must contain at least one seat")
        object.__setattr__(self, "movie", self.movie.strip())
        object.__setattr__(self, "screen", self.screen.strip())
        object.__setattr__(self, "seats", normalized_seats)


@dataclass(frozen=True)
class SeatHold:
    id: str
    show_id: str
    seats: frozenset[str]
    expires_at: datetime

    def __post_init__(self) -> None:
        if not self.id.strip() or not self.show_id.strip():
            raise InvalidBookingError("Hold id and show id are required")
        if not self.seats:
            raise InvalidBookingError("A hold requires at least one seat")

    def is_expired(self, now: datetime) -> bool:
        return now >= self.expires_at


@dataclass(frozen=True)
class Booking:
    id: str
    show_id: str
    hold_id: str
    customer_name: str
    seats: frozenset[str]
    total: Decimal

    def __post_init__(self) -> None:
        customer_name = self.customer_name.strip()
        if not self.id.strip() or not self.show_id.strip() or not self.hold_id.strip():
            raise InvalidBookingError("Booking, show, and hold ids are required")
        if len(customer_name) < 2:
            raise InvalidBookingError(
                "Customer name must contain at least two characters"
            )
        if not self.seats:
            raise InvalidBookingError("A booking requires at least one seat")
        if self.total <= Decimal("0"):
            raise InvalidBookingError("Booking total must be positive")
        object.__setattr__(self, "customer_name", customer_name)
