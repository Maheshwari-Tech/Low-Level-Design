from dataclasses import dataclass
from datetime import date, datetime, time, timedelta

from app.exceptions.calendar_exceptions import CalendarError
from app.models.time_range import TimeRange


@dataclass(frozen=True)
class SchedulingPolicy:
    work_start: time = time(9, 0)
    work_end: time = time(17, 0)
    slot_duration: timedelta = timedelta(minutes=30)

    def __post_init__(self) -> None:
        if self.work_end <= self.work_start:
            raise CalendarError("Working day must end after it starts")
        if self.slot_duration <= timedelta(0):
            raise CalendarError("Slot duration must be positive")

    def validate(self, meeting_time: TimeRange) -> None:
        day_start = datetime.combine(meeting_time.start.date(), self.work_start)
        day_end = datetime.combine(meeting_time.start.date(), self.work_end)
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
