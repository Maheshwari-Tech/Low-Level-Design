from fastapi import Request
from fastapi.responses import JSONResponse

from app.exceptions.game_exceptions import (
    BattleshipError,
    InvalidPlacementError,
    InvalidShotError,
    UnknownShipError,
)

BAD_REQUEST_ERRORS = (InvalidPlacementError, InvalidShotError, UnknownShipError)


async def battleship_error_handler(
    _request: Request,
    error: BattleshipError,
) -> JSONResponse:
    status_code = 400 if isinstance(error, BAD_REQUEST_ERRORS) else 409
    return JSONResponse(status_code=status_code, content={"detail": str(error)})
