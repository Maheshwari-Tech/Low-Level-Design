from dataclasses import dataclass
from decimal import Decimal

from app.exceptions.booking_exceptions import InvalidBookingError


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
