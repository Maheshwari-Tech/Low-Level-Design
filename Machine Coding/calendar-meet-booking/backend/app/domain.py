from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from typing import Iterable


class CalendarError(ValueError):
    pass


class MeetingConflictError(CalendarError):
    pass


def normalize_participants(participants: Iterable[str]) -> frozenset[str]:
    """Return the canonical participant identity used for conflict checks."""
    return frozenset(
        participant.strip().casefold()
        for participant in participants
        if participant.strip()
    )


@dataclass(frozen=True)
class TimeRange:
    start: datetime
    end: datetime

    def __post_init__(self) -> None:
        if self.start.utcoffset() is not None or self.end.utcoffset() is not None:
            raise CalendarError(
                "Use local calendar times without an offset; time-zone conversion is out of scope"
            )
        if self.end <= self.start:
            raise CalendarError("Meeting end must be after its start")

    def overlaps(self, other: "TimeRange") -> bool:
        return self.start < other.end and self.end > other.start


@dataclass(frozen=True)
class Meeting:
    id: str
    title: str
    participants: frozenset[str]
    time: TimeRange

    def __post_init__(self) -> None:
        normalized_title = self.title.strip()
        normalized_participants = normalize_participants(self.participants)
        if not self.id.strip():
            raise CalendarError("Meeting id is required")
        if len(normalized_title) < 2:
            raise CalendarError("Title must contain at least two characters")
        if not normalized_participants:
            raise CalendarError("At least one participant is required")
        object.__setattr__(self, "title", normalized_title)
        object.__setattr__(self, "participants", normalized_participants)

    def conflicts_with(self, other: "Meeting") -> bool:
        return bool(
            self.participants.intersection(other.participants)
        ) and self.time.overlaps(other.time)


@dataclass(frozen=True)
class SchedulingPolicy:
    """Configurable business rules for bookable time and availability slots."""

    work_start: time = time(9, 0)
    work_end: time = time(17, 0)
    slot_duration: timedelta = timedelta(minutes=30)

    def __post_init__(self) -> None:
        if self.work_end <= self.work_start:
            raise CalendarError("Working day must end after it starts")
        if self.slot_duration <= timedelta(0):
            raise CalendarError("Slot duration must be positive")

    def validate(self, meeting_time: TimeRange) -> None:
        day_start = datetime.combine(
            meeting_time.start.date(), self.work_start, meeting_time.start.tzinfo
        )
        day_end = datetime.combine(
            meeting_time.start.date(), self.work_end, meeting_time.start.tzinfo
        )
        if meeting_time.end.date() != meeting_time.start.date():
            raise CalendarError("Meeting must start and end on the same day")
        if meeting_time.start < day_start or meeting_time.end > day_end:
            raise CalendarError("Meeting must be within working hours")

    def slots_for(self, day: date) -> tuple[TimeRange, ...]:
        cursor = datetime.combine(day, self.work_start)
        end_of_day = datetime.combine(day, self.work_end)
        slots: list[TimeRange] = []
        while cursor + self.slot_duration <= end_of_day:
            slots.append(TimeRange(cursor, cursor + self.slot_duration))
            cursor += self.slot_duration
        return tuple(slots)
