from dataclasses import dataclass

from app.models.time_range import TimeRange


@dataclass(frozen=True)
class AvailabilitySlot:
    time_range: TimeRange
    available: bool
