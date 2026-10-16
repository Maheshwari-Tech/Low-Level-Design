from dataclasses import dataclass

from app.exceptions.calendar_exceptions import CalendarError
from app.models.participant import Participant
from app.models.time_range import TimeRange


@dataclass(frozen=True)
class Meeting:
    id: str
    title: str
    participants: frozenset[Participant]
    time_range: TimeRange

    def __post_init__(self) -> None:
        meeting_id = self.id.strip()
        title = self.title.strip()
        if not meeting_id:
            raise CalendarError("Meeting id is required")
        if len(title) < 2:
            raise CalendarError("Title must contain at least two characters")
        if not self.participants:
            raise CalendarError("At least one participant is required")

        object.__setattr__(self, "id", meeting_id)
        object.__setattr__(self, "title", title)

    def conflicts_with(self, other: "Meeting") -> bool:
        shares_participant = bool(self.participants.intersection(other.participants))
        return shares_participant and self.time_range.overlaps(other.time_range)
