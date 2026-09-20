from __future__ import annotations

from datetime import date, datetime
from typing import Callable
from uuid import uuid4

from .domain import (
    CalendarError,
    Meeting,
    MeetingConflictError,
    SchedulingPolicy,
    TimeRange,
    normalize_participants,
)
from .ports import CalendarRepository

IdGenerator = Callable[[], str]


class CalendarService:
    def __init__(
        self,
        repository: CalendarRepository,
        policy: SchedulingPolicy | None = None,
        id_generator: IdGenerator | None = None,
    ) -> None:
        self._repository = repository
        self._policy = policy or SchedulingPolicy()
        self._id_generator = id_generator or (lambda: str(uuid4()))

    def list_meetings(self) -> tuple[Meeting, ...]:
        return self._repository.list_meetings()

    def schedule(
        self, title: str, participants: list[str], start: datetime, end: datetime
    ) -> Meeting:
        normalized_participants = normalize_participants(participants)
        if not normalized_participants:
            raise CalendarError("At least one participant is required")
        meeting_time = TimeRange(start, end)
        self._policy.validate(meeting_time)
        meeting = Meeting(
            self._id_generator(), title, normalized_participants, meeting_time
        )
        if not self._repository.add_if_no_conflict(meeting):
            raise MeetingConflictError(
                "One or more participants already have a meeting in this time range"
            )
        return meeting

    def availability(
        self, day: date, participants: list[str]
    ) -> tuple[tuple[TimeRange, bool], ...]:
        people = normalize_participants(participants)
        if not people:
            raise CalendarError("At least one participant is required")
        meetings = self._repository.list_meetings()
        return tuple(
            (
                slot,
                not any(
                    people.intersection(meeting.participants)
                    and slot.overlaps(meeting.time)
                    for meeting in meetings
                ),
            )
            for slot in self._policy.slots_for(day)
        )
