from datetime import date

from fastapi import APIRouter, Query

from app.api.schemas import CreateMeetingRequest
from app.controllers.calendar_controller import CalendarController


def create_router(controller: CalendarController) -> APIRouter:
    router = APIRouter(prefix="/api", tags=["calendar"])

    @router.get("/meetings")
    def list_meetings() -> list[dict[str, object]]:
        return controller.list_meetings()

    @router.get("/availability")
    def get_availability(
        day: date = Query(),
        participants: str = Query(),
    ) -> dict[str, object]:
        return controller.get_availability(day, participants.split(","))

    @router.post("/meetings", status_code=201)
    def create_meeting(request: CreateMeetingRequest) -> dict[str, object]:
        return controller.schedule_meeting(
            request.title,
            request.participants,
            request.start,
            request.end,
        )

    return router
