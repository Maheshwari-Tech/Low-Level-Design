from dataclasses import dataclass
from datetime import datetime

from app.exceptions.calendar_exceptions import CalendarError


@dataclass(frozen=True)
class TimeRange:
    start: datetime
    end: datetime

    def __post_init__(self) -> None:
        if self.start.utcoffset() is not None or self.end.utcoffset() is not None:
            raise CalendarError(
                "Use local calendar times without an offset; "
                "time-zone conversion is out of scope"
            )
        if self.end <= self.start:
            raise CalendarError("Meeting end must be after its start")

    def overlaps(self, other: "TimeRange") -> bool:
        return self.start < other.end and self.end > other.start
