from datetime import datetime
from typing import Protocol

from app.models.booking import Booking
from app.models.seat_hold import SeatHold
from app.models.seat_inventory import SeatInventory
from app.models.show import Show


class BookingRepository(Protocol):
    def list_shows(self) -> tuple[Show, ...]: ...

    def get_show(self, show_id: str) -> Show | None: ...

    def get_hold(self, hold_id: str) -> SeatHold | None: ...

    def get_seat_inventory(self, show_id: str, now: datetime) -> SeatInventory: ...

    def save_hold_if_available(self, hold: SeatHold, now: datetime) -> bool: ...

    def save_booking_if_hold_active(
        self, booking: Booking, now: datetime
    ) -> bool: ...
