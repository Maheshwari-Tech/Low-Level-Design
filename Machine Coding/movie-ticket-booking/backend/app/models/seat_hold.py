from dataclasses import dataclass
from datetime import datetime

from app.exceptions.booking_exceptions import InvalidBookingError


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
