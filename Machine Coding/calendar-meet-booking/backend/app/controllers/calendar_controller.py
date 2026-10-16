from datetime import date, datetime

from app.models.availability_slot import AvailabilitySlot
from app.models.meeting import Meeting
from app.services.calendar_service import CalendarService


class CalendarController:
    def __init__(self, service: CalendarService) -> None:
        self._service = service

    def list_meetings(self) -> list[dict[str, object]]:
        return [
            self._serialize_meeting(meeting)
            for meeting in self._service.list_meetings()
        ]

    def get_availability(
        self,
        day: date,
        participant_emails: list[str],
    ) -> dict[str, object]:
        slots = self._service.availability(day, participant_emails)
        participants = sorted(
            {
                email.strip().casefold()
                for email in participant_emails
                if email.strip()
            }
        )
        return {
            "day": day,
            "participants": participants,
            "slots": [self._serialize_slot(slot) for slot in slots],
        }

    def schedule_meeting(
        self,
        title: str,
        participants: list[str],
        start: datetime,
        end: datetime,
    ) -> dict[str, object]:
        meeting = self._service.schedule(title, participants, start, end)
        return self._serialize_meeting(meeting)

    @staticmethod
    def _serialize_meeting(meeting: Meeting) -> dict[str, object]:
        return {
            "id": meeting.id,
            "title": meeting.title,
            "participants": sorted(
                participant.email for participant in meeting.participants
            ),
            "start": meeting.time_range.start,
            "end": meeting.time_range.end,
        }

    @staticmethod
    def _serialize_slot(slot: AvailabilitySlot) -> dict[str, object]:
        return {
            "start": slot.time_range.start,
            "end": slot.time_range.end,
            "available": slot.available,
        }
