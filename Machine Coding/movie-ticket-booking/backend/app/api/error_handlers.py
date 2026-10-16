from fastapi import Request
from fastapi.responses import JSONResponse

from app.exceptions.booking_exceptions import (
    BookingError,
    HoldExpiredError,
    HoldNotFoundError,
    InvalidBookingError,
    ShowNotFoundError,
)


async def booking_error_handler(
    _request: Request,
    error: BookingError,
) -> JSONResponse:
    if isinstance(error, (ShowNotFoundError, HoldNotFoundError)):
        status_code = 404
    elif isinstance(error, HoldExpiredError):
        status_code = 410
    elif isinstance(error, InvalidBookingError):
        status_code = 400
    else:
        status_code = 409
    return JSONResponse(status_code=status_code, content={"detail": str(error)})
