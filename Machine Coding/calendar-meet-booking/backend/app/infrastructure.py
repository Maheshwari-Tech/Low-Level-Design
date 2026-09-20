from __future__ import annotations

from datetime import datetime
from threading import RLock

from .domain import Meeting, TimeRange


class InMemoryCalendarRepository:
    def __init__(self, meetings: tuple[Meeting, ...] = ()) -> None:
        self._meetings = {meeting.id: meeting for meeting in meetings}
        self._lock = RLock()

    def list_meetings(self) -> tuple[Meeting, ...]:
        with self._lock:
            return tuple(
                sorted(self._meetings.values(), key=lambda meeting: meeting.time.start)
            )

    def add_if_no_conflict(self, meeting: Meeting) -> bool:
        with self._lock:
            if any(
                existing.conflicts_with(meeting) for existing in self._meetings.values()
            ):
                return False
            self._meetings[meeting.id] = meeting
            return True


def seeded_repository() -> InMemoryCalendarRepository:
    meetings = (
        Meeting(
            "meeting-1",
            "Design review",
            frozenset({"ava@demo.com", "sam@demo.com"}),
            TimeRange(
                datetime.fromisoformat("2026-09-22T10:00:00"),
                datetime.fromisoformat("2026-09-22T10:30:00"),
            ),
        ),
        Meeting(
            "meeting-2",
            "Team stand-up",
            frozenset({"sam@demo.com"}),
            TimeRange(
                datetime.fromisoformat("2026-09-22T14:00:00"),
                datetime.fromisoformat("2026-09-22T14:30:00"),
            ),
        ),
    )
    return InMemoryCalendarRepository(meetings)
