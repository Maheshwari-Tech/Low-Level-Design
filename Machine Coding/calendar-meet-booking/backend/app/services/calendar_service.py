from collections.abc import Callable
from datetime import date, datetime
from uuid import uuid4

from app.exceptions.calendar_exceptions import CalendarError, MeetingConflictError
from app.models.availability_slot import AvailabilitySlot
from app.models.meeting import Meeting
from app.models.participant import Participant
from app.models.scheduling_policy import SchedulingPolicy
from app.models.time_range import TimeRange
from app.repositories.calendar_repository import CalendarRepository

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
        self,
        title: str,
        participant_emails: list[str],
        start: datetime,
        end: datetime,
    ) -> Meeting:
        participants = self._create_participants(participant_emails)
        time_range = TimeRange(start, end)
        self._policy.validate(time_range)
        meeting = Meeting(
            id=self._id_generator(),
            title=title,
            participants=participants,
            time_range=time_range,
        )
        if not self._repository.add_if_no_conflict(meeting):
            raise MeetingConflictError(
                "One or more participants already have a meeting in this time range"
            )
        return meeting

    def availability(
        self,
        day: date,
        participant_emails: list[str],
    ) -> tuple[AvailabilitySlot, ...]:
        participants = self._create_participants(participant_emails)
        meetings = self._repository.list_meetings()
        return tuple(
            AvailabilitySlot(
                time_range=slot,
                available=not any(
                    participants.intersection(meeting.participants)
                    and slot.overlaps(meeting.time_range)
                    for meeting in meetings
                ),
            )
            for slot in self._policy.slots_for(day)
        )

    @staticmethod
    def _create_participants(emails: list[str]) -> frozenset[Participant]:
        participants = frozenset(
            Participant(email) for email in emails if email.strip()
        )
        if not participants:
            raise CalendarError("At least one participant is required")
        return participants
