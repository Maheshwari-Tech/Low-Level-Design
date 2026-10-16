from dataclasses import dataclass

from app.models.enums import SeatStatus


@dataclass(frozen=True)
class SeatInventory:
    held_seats: frozenset[str]
    booked_seats: frozenset[str]

    def status_of(self, seat: str) -> SeatStatus:
        if seat in self.booked_seats:
            return SeatStatus.BOOKED
        if seat in self.held_seats:
            return SeatStatus.HELD
        return SeatStatus.AVAILABLE
