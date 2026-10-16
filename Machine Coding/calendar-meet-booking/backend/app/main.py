from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.error_handlers import calendar_error_handler
from app.api.routes import create_router
from app.controllers.calendar_controller import CalendarController
from app.exceptions.calendar_exceptions import CalendarError
from app.repositories.in_memory_calendar_repository import (
    seeded_calendar_repository,
)
from app.services.calendar_service import CalendarService


def create_app() -> FastAPI:
    application = FastAPI(title="Calendar Meet Booking API", version="1.0.0")
    application.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5103"],
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type"],
    )
    service = CalendarService(seeded_calendar_repository())
    application.include_router(create_router(CalendarController(service)))
    application.add_exception_handler(CalendarError, calendar_error_handler)
    return application


app = create_app()
