from enum import StrEnum


class SeatStatus(StrEnum):
    AVAILABLE = "available"
    HELD = "held"
    BOOKED = "booked"
