from typing import Protocol

from .domain import Meeting


class CalendarRepository(Protocol):
    def list_meetings(self) -> tuple[Meeting, ...]: ...
    def add_if_no_conflict(self, meeting: Meeting) -> bool: ...
