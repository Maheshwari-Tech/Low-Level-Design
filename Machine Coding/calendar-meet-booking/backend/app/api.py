from datetime import date, datetime

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

from .application import CalendarService
from .domain import CalendarError, Meeting, MeetingConflictError


class CreateMeetingRequest(BaseModel):
    title: str = Field(min_length=2, max_length=100)
    participants: list[str] = Field(min_length=1)
    start: datetime
    end: datetime


def create_router(service: CalendarService) -> APIRouter:
    router = APIRouter(prefix="/api")

    @router.get("/meetings")
    def list_meetings():
        return [serialize(meeting) for meeting in service.list_meetings()]

    @router.get("/availability")
    def availability(day: date = Query(), participants: str = Query()):
        people = participants.split(",")
        try:
            slots = service.availability(day, people)
            return {
                "day": day,
                "participants": sorted(
                    person.strip().casefold() for person in people if person.strip()
                ),
                "slots": [
                    {"start": slot.start, "end": slot.end, "available": available}
                    for slot, available in slots
                ],
            }
        except CalendarError as error:
            raise HTTPException(400, str(error)) from error

    @router.post("/meetings", status_code=201)
    def create_meeting(request: CreateMeetingRequest):
        try:
            return serialize(
                service.schedule(
                    request.title, request.participants, request.start, request.end
                )
            )
        except CalendarError as error:
            raise HTTPException(
                409 if isinstance(error, MeetingConflictError) else 400, str(error)
            ) from error

    return router


def serialize(meeting: Meeting) -> dict[str, object]:
    return {
        "id": meeting.id,
        "title": meeting.title,
        "participants": sorted(meeting.participants),
        "start": meeting.time.start,
        "end": meeting.time.end,
    }
