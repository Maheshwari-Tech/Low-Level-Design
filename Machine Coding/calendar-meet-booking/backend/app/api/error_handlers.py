from fastapi import Request
from fastapi.responses import JSONResponse

from app.exceptions.calendar_exceptions import CalendarError, MeetingConflictError


async def calendar_error_handler(
    _request: Request,
    error: CalendarError,
) -> JSONResponse:
    status_code = 409 if isinstance(error, MeetingConflictError) else 400
    return JSONResponse(status_code=status_code, content={"detail": str(error)})
