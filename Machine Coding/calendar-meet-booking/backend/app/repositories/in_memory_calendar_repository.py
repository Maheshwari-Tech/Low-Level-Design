from datetime import datetime
from threading import Lock

from app.models.meeting import Meeting
from app.models.participant import Participant
from app.models.time_range import TimeRange


class InMemoryCalendarRepository:
    def __init__(self, meetings: tuple[Meeting, ...] = ()) -> None:
        self._meetings = {meeting.id: meeting for meeting in meetings}
        self._lock = Lock()

    def list_meetings(self) -> tuple[Meeting, ...]:
        with self._lock:
            return tuple(
                sorted(
                    self._meetings.values(),
                    key=lambda meeting: meeting.time_range.start,
                )
            )

    def add_if_no_conflict(self, meeting: Meeting) -> bool:
        with self._lock:
            if any(
                existing.conflicts_with(meeting)
                for existing in self._meetings.values()
            ):
                return False
            self._meetings[meeting.id] = meeting
            return True


def seeded_calendar_repository() -> InMemoryCalendarRepository:
    return InMemoryCalendarRepository(
        (
            Meeting(
                id="meeting-1",
                title="Design review",
                participants=frozenset(
                    {Participant("ava@demo.com"), Participant("sam@demo.com")}
                ),
                time_range=TimeRange(
                    datetime.fromisoformat("2026-09-22T10:00:00"),
                    datetime.fromisoformat("2026-09-22T10:30:00"),
                ),
            ),
            Meeting(
                id="meeting-2",
                title="Team stand-up",
                participants=frozenset({Participant("sam@demo.com")}),
                time_range=TimeRange(
                    datetime.fromisoformat("2026-09-22T14:00:00"),
                    datetime.fromisoformat("2026-09-22T14:30:00"),
                ),
            ),
        )
    )
