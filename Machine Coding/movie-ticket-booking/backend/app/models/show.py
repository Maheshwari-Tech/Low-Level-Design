from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal

from app.exceptions.booking_exceptions import InvalidBookingError


@dataclass(frozen=True)
class Show:
    id: str
    movie: str
    screen: str
    starts_at: datetime
    price: Decimal
    seats: frozenset[str]

    def __post_init__(self) -> None:
        show_id = self.id.strip()
        movie = self.movie.strip()
        screen = self.screen.strip()
        seats = frozenset(seat.strip().upper() for seat in self.seats if seat.strip())

        if not show_id or not movie or not screen:
            raise InvalidBookingError("Show id, movie, and screen are required")
        if self.price <= Decimal("0"):
            raise InvalidBookingError("Show price must be positive")
        if not seats:
            raise InvalidBookingError("Show must contain at least one seat")

        object.__setattr__(self, "id", show_id)
        object.__setattr__(self, "movie", movie)
        object.__setattr__(self, "screen", screen)
        object.__setattr__(self, "seats", seats)
